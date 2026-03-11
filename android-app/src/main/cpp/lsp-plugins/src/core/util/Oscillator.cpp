/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Stefano Tronci <stefano.tronci@protonmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 20 Mar 2017
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
#include <core/util/Oscillator.h>

namespace lsp
{
    Oscillator::Oscillator()
    {
        enFunction                  = FG_SINE;
        fAmplitude                  = 1.0f;
        fFrequency                  = 0.0f;
        fDCOffset                   = 0.0f;
        enDCReference               = DC_WAVEDC;
        fReferencedDC               = 0.0f;
        fInitPhase                  = 0.0f;

        nSampleRate                 = -1;
        nPhaseAcc                   = 0;
        nPhaseAccBits               = sizeof(phacc_t) * 8;
        nPhaseAccMaxBits            = sizeof(phacc_t) * 8;
        nPhaseAccMask               = 0;
        fAcc2Phase                  = 0.0f;

        nFreqCtrlWord               = 0;
        nInitPhaseWord              = 0;

        sSquaredSinusoid.bInvert    = false;
        sSquaredSinusoid.fAmplitude = 0.0f;
        sSquaredSinusoid.fWaveDC    = 0.0f;

        sRectangular.fDutyRatio     = 0.5f;
        sRectangular.nDutyWord      = 0;
        sRectangular.fWaveDC        = 0.0f;
        sRectangular.fBLPeakAtten   = 0.0f;

        sSawtooth.fWidth            = 1.0f;
        sSawtooth.nWidthWord        = 0;
        sSawtooth.fCoeffs[0]        = 0.0f;
        sSawtooth.fCoeffs[1]        = 0.0f;
        sSawtooth.fCoeffs[2]        = 0.0f;
        sSawtooth.fCoeffs[3]        = 0.0f;
        sSawtooth.fWaveDC           = 0.0f;
        sSawtooth.fBLPeakAtten      = 0.0f;

        sTrapezoid.fRaiseRatio      = 0.25f;
        sTrapezoid.fFallRatio       = 0.25f;
        sTrapezoid.nPoints[0]       = 0;
        sTrapezoid.nPoints[1]       = 0;
        sTrapezoid.nPoints[2]       = 0;
        sTrapezoid.nPoints[3]       = 0;
        sTrapezoid.fCoeffs[0]       = 0.0f;
        sTrapezoid.fCoeffs[1]       = 0.0f;
        sTrapezoid.fCoeffs[2]       = 0.0f;
        sTrapezoid.fCoeffs[3]       = 0.0f;
        sTrapezoid.fWaveDC          = 0.0f;
        sTrapezoid.fBLPeakAtten     = 0.0f;
    }

    Oscillator::~Oscillator()
    {
    }

    void Oscillator::set_sample_rate(size_t sr)
    {
        nSampleRate = sr;
    }

    void Oscillator::set_frequency(float freq)
    {
        fFrequency = freq;
    }

    void Oscillator::set_amplitude(float amp)
    {
        fAmplitude = amp;
    }

    void Oscillator::set_dc_offset(float offset)
    {
        fDCOffset = offset;
    }

    void Oscillator::set_function(func_generator_t func)
    {
        enFunction = func;
    }

} /* namespace lsp */
