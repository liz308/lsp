/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Stefano Tronci <stefano.tronci@protonmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 12 Jul 2017
 *
 * lsp-plugins is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * lsp-plugins is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with lsp-plugins. If not, see <https://www.gnu.org/licenses/>.
 */

#include <dsp/dsp.h>
#include <core/debug.h>
#include <core/util/SyncChirpProcessor.h>
#include <math.h>
#include <stdlib.h>

#define MIN_AMPLITUDE               1.0e-6f
#define DFL_TAIL                    1.0f
#define MAX_TAIL                    5.0f
#define DFL_DURATION                5.0f
#define LIM_DURATION                50.0f
#define LIM_OPT_ADDTIME             10.0f
#define LIM_LAG                     (1 << 7)
#define ASYM_THRS                   10
#define DFL_INITIAL_FREQ            1.0f
#define DFL_FINAL_FREQ              8000.0f
#define MAX_PART_SIZE           	32768
#define FADEIN_FRACTION             0.3f
#define FADEOUT_FRACTION            0.3f
#define OVER_BUF_LIMIT_SIZE         (12 * 1024)
#define ENVELOPE_BUF_LIMIT_SIZE     65536
#define BG_NOISE_LIMIT             -10.0
#define MAX_WINDOW_RANK             16

namespace lsp
{
    SyncChirpProcessor::SyncChirpProcessor()
    {
        nSampleRate                     = -1;

        sChirpParams.enMethod           = SCP_SYNTH_SIMPLE;
        sChirpParams.initialFrequency   = DFL_INITIAL_FREQ;
        sChirpParams.finalFrequency     = DFL_FINAL_FREQ;
        sChirpParams.fDuration          = 0.0f;
        sChirpParams.fAlpha             = 1.0f;

        sChirpParams.fDurationCoarse    = 0.0f;
        sChirpParams.nDuration          = 0;
        sChirpParams.nTimeLags          = 0;
        sChirpParams.nOrder             = 0;
        sChirpParams.beta               = 0.0;
        sChirpParams.gamma              = 0.0;
        sChirpParams.delta              = 0.0;
        sChirpParams.fConvScale         = 0.0f;

        sChirpParams.bAsymptotic        = false;
        sChirpParams.bRecalculate       = true;
        sChirpParams.bReconfigure       = true;

        sFader.enMethod                 = SCP_FADE_NONE;
        sFader.fFadeIn                  = 0.0f;
        sFader.fFadeOut                 = 0.0f;
        sFader.nFadeIn                  = 0;
        sFader.nFadeOut                 = 0;
        sFader.nFadeIn_Over             = 0;
        sFader.nFadeOut_Over            = 0;

        sConvParams.nChannels			= 0;
        sConvParams.nPartitionSize		= 0;
        sConvParams.nConvRank			= 0;
        sConvParams.nImage				= 0;
        sConvParams.nAllocationSize		= 0;
        sConvParams.vPartitions 		= NULL;
        sConvParams.vPaddedLengths		= NULL;
        sConvParams.vInversePrepends    = NULL;
        sConvParams.vConvLengths        = NULL;
        sConvParams.vAlignOffsets       = NULL;
        sConvParams.pData				= NULL;
        sConvParams.vInPart 			= NULL;
        sConvParams.vInvPart 			= NULL;
        sConvParams.vInImage 			= NULL;
        sConvParams.vInvImage 			= NULL;
        sConvParams.vTemp 				= NULL;
        sConvParams.pTempData 			= NULL;
        sConvParams.bReallocateTemp 	= true;

        sCRPostProc.noiseLevel          = 0.0;
        sCRPostProc.noiseValue          = 0.0;
        sCRPostProc.fIrLimit            = 0.0f;
        sCRPostProc.nIrLimit            = 0;
        sCRPostProc.noiseLevelNorm      = 0.0;
        sCRPostProc.noiseValueNorm      = 0.0;
        sCRPostProc.bLowNoise           = false;
        sCRPostProc.nRT                 = 0;
        sCRPostProc.fRT                 = 0.0f;
        sCRPostProc.fCorrelation        = 0.0f;
        sCRPostProc.nHamOrder           = 0;
        sCRPostProc.nHwinSize           = 0;
        sCRPostProc.nWinRank            = 0;
        sCRPostProc.mCoeffsReDet        = 0.0f;
        sCRPostProc.mCoeffsImDet        = 0.0f;
        sCRPostProc.mCoeffsRe           = NULL;
        sCRPostProc.mCoeffsIm           = NULL;
        sCRPostProc.mHigherRe           = NULL;
        sCRPostProc.mHigherIm           = NULL;
        sCRPostProc.mKernelsRe          = NULL;
        sCRPostProc.mKernelsIm          = NULL;
        sCRPostProc.vTemprow1Re         = NULL;
        sCRPostProc.vTemprow1Im         = NULL;
        sCRPostProc.vTemprow2Re         = NULL;
        sCRPostProc.vTemprow2Im         = NULL;
        sCRPostProc.pData               = NULL;

        pChirp                          = NULL;
        pInverseFilter                  = NULL;
        pConvResult                     = NULL;

        enOverMode                      = OM_LANCZOS_8X2;
        nOversampling                   = 0;

        vOverBuffer1                    = NULL;
        vOverBuffer2                    = NULL;
        vEnvelopeBuffer                 = NULL;
        pData                           = NULL;

        bSync                           = true;
    }

    SyncChirpProcessor::~SyncChirpProcessor()
    {
    }

    bool SyncChirpProcessor::init()
    {
        pChirp          = new Sample();
        pInverseFilter  = new Sample();
        pConvResult     = new AudioFile();

        size_t samples  = 2 * OVER_BUF_LIMIT_SIZE + ENVELOPE_BUF_LIMIT_SIZE;

        float *ptr              = alloc_aligned<float>(pData, samples);
        if (ptr == NULL)
            return false;

        vOverBuffer1    = ptr;
        ptr            += OVER_BUF_LIMIT_SIZE;
        vOverBuffer2    = ptr;
        ptr            += OVER_BUF_LIMIT_SIZE;
        vEnvelopeBuffer = ptr;
        ptr            += ENVELOPE_BUF_LIMIT_SIZE;

        return sOver1.init() && sOver2.init();
    }

    void SyncChirpProcessor::destroy()
    {
    	destroyConvolutionParameters();
    	destroyConvolutionTempArrays();
    	destroyIdentificationMatrices();

        if (pChirp != NULL)
        {
            delete pChirp;
            pChirp = NULL;
        }

        if (pInverseFilter != NULL)
        {
            delete pInverseFilter;
            pInverseFilter = NULL;
        }

        if (pConvResult != NULL)
        {
            pConvResult->destroy();
            delete pConvResult;
            pConvResult = NULL;
        }

        free_aligned(pData);
        pData          	= NULL;
        vOverBuffer1   	= NULL;
        vOverBuffer2  	= NULL;
        vEnvelopeBuffer	= NULL;

        sOver1.destroy();
        sOver2.destroy();
    }

    void SyncChirpProcessor::destroyIdentificationMatrices()
    {
        free_aligned(sCRPostProc.pData);
        sCRPostProc.pData   = NULL;
        sCRPostProc.nHamOrder   = 0;
        sCRPostProc.nHwinSize   = 0;
        sCRPostProc.mCoeffsRe   = NULL;
        sCRPostProc.mCoeffsIm   = NULL;
        sCRPostProc.mHigherRe   = NULL;
        sCRPostProc.mHigherIm   = NULL;
        sCRPostProc.mKernelsRe  = NULL;
        sCRPostProc.mKernelsIm  = NULL;
        sCRPostProc.vTemprow1Re = NULL;
        sCRPostProc.vTemprow1Im = NULL;
        sCRPostProc.vTemprow2Re = NULL;
        sCRPostProc.vTemprow2Im = NULL;
    }

    void SyncChirpProcessor::destroyConvolutionParameters()
    {
    	free_aligned(sConvParams.pData);
    	sConvParams.pData				= NULL;
        sConvParams.vPartitions 		= NULL;
        sConvParams.vPaddedLengths		= NULL;
        sConvParams.vInversePrepends    = NULL;
        sConvParams.vConvLengths        = NULL;
        sConvParams.vAlignOffsets       = NULL;
    }

    void SyncChirpProcessor::destroyConvolutionTempArrays()
    {
    	free_aligned(sConvParams.pTempData);
    	sConvParams.pTempData 	= NULL;
    	sConvParams.vInPart     = NULL;
    	sConvParams.vInvPart    = NULL;
    	sConvParams.vInImage    = NULL;
    	sConvParams.vInvImage   = NULL;
    	sConvParams.vTemp       = NULL;
    }

    void SyncChirpProcessor::set_sample_rate(size_t sr)
    {
        nSampleRate = sr;
        bSync = true;
    }

    void SyncChirpProcessor::update_settings()
    {
        if (!bSync)
            return;
        bSync = false;
    }

} /* namespace lsp */
