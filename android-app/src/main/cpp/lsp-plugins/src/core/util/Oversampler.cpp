/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 19 нояб. 2016 г.
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

#include <core/util/Oversampler.h>

#define OS_UP_BUFFER_SIZE       (12 * 1024)
#define OS_DOWN_BUFFER_SIZE     (12 * 1024)
#define OS_CUTOFF               21000.0f

namespace lsp
{
    IOversamplerCallback::~IOversamplerCallback()
    {
    }

    void IOversamplerCallback::process(float *out, const float *in, size_t samples)
    {
        dsp::copy(out, in, samples);
    }

    Oversampler::Oversampler()
    {
        construct();
    }

    Oversampler::~Oversampler()
    {
        destroy();
    }

    void Oversampler::construct()
    {
        pCallback   = NULL;
        fUpBuffer   = NULL;
        fDownBuffer = NULL;
        nUpHead     = 0;
        nMode       = OM_NONE;
        nSampleRate = 0;
        nUpdate     = UP_ALL;
        bData       = NULL;
        bFilter     = true;
    }

    bool Oversampler::init()
    {
        if (!sFilter.init(NULL))
            return false;

        if (bData == NULL)
        {
            size_t samples  = OS_UP_BUFFER_SIZE + OS_DOWN_BUFFER_SIZE + RESAMPLING_RESERVED_SAMPLES;
            bData           = new uint8_t[samples * sizeof(float) + DEFAULT_ALIGN];
            if (bData == NULL)
                return false;
            float *ptr      = reinterpret_cast<float *>(ALIGN_PTR(bData, DEFAULT_ALIGN));
            fDownBuffer     = ptr;
            ptr            += OS_DOWN_BUFFER_SIZE;
            fUpBuffer       = reinterpret_cast<float *>(ptr);
            ptr            += OS_UP_BUFFER_SIZE + RESAMPLING_RESERVED_SAMPLES;
        }

        // Clear buffer
        dsp::fill_zero(fUpBuffer, OS_UP_BUFFER_SIZE + RESAMPLING_RESERVED_SAMPLES);
        dsp::fill_zero(fDownBuffer, OS_DOWN_BUFFER_SIZE);
        nUpHead       = 0;

        return true;
    }

    void Oversampler::destroy()
    {
        sFilter.destroy();
        if (bData != NULL)
        {
            delete [] bData;
            fUpBuffer   = NULL;
            fDownBuffer = NULL;
            bData       = NULL;
        }
        pCallback = NULL;
    }

    void Oversampler::set_sample_rate(size_t sr)
    {
        if (sr == nSampleRate)
            return;
        nSampleRate     = sr;
        nUpdate        |= UP_SAMPLE_RATE;
        size_t os       = get_oversampling();

        // Update filter parameters
        filter_params_t fp;
        fp.fFreq        = OS_CUTOFF;
        fp.fFreq2       = fp.fFreq;
        fp.fGain        = 1.0f;
        fp.fQuality     = 0.5f;
        fp.nSlope       = 30;
        fp.nType        = FLT_BT_BWC_LOPASS;

        sFilter.update(nSampleRate * os, &fp);
    }

    void Oversampler::update_settings()
    {
        if (nUpdate & (UP_MODE | UP_SAMPLE_RATE))
        {
            dsp::fill_zero(fUpBuffer, OS_UP_BUFFER_SIZE + RESAMPLING_RESERVED_SAMPLES);
            nUpHead       = 0;
            sFilter.clear();
        }

        size_t os       = get_oversampling();
        filter_params_t fp;
        sFilter.get_params(&fp);
        sFilter.update(nSampleRate * os, &fp);

        nUpdate = 0;
        return;
    }

    size_t Oversampler::get_oversampling() const
    {
        switch (nMode)
        {
            case OM_LANCZOS_2X2:
            case OM_LANCZOS_2X3:
            case OM_LANCZOS_2X4:
                return 2;

            case OM_LANCZOS_3X2:
            case OM_LANCZOS_3X3:
            case OM_LANCZOS_3X4:
                return 3;

            case OM_LANCZOS_4X2:
            case OM_LANCZOS_4X3:
            case OM_LANCZOS_4X4:
                return 4;

            case OM_LANCZOS_6X2:
            case OM_LANCZOS_6X3:
            case OM_LANCZOS_6X4:
                return 6;

            case OM_LANCZOS_8X2:
            case OM_LANCZOS_8X3:
            case OM_LANCZOS_8X4:
                return 8;

            default:
                break;
        }

        return 1;
    }

    size_t Oversampler::latency() const
    {
        switch (nMode)
        {
            case OM_LANCZOS_2X2:
            case OM_LANCZOS_3X2:
            case OM_LANCZOS_4X2:
            case OM_LANCZOS_6X2:
            case OM_LANCZOS_8X2:
                return 2;

            case OM_LANCZOS_2X3:
            case OM_LANCZOS_3X3:
            case OM_LANCZOS_4X3:
            case OM_LANCZOS_6X3:
            case OM_LANCZOS_8X3:
                return 3;

            case OM_LANCZOS_2X4:
            case OM_LANCZOS_3X4:
            case OM_LANCZOS_4X4:
            case OM_LANCZOS_6X4:
            case OM_LANCZOS_8X4:
                return 4;

            default:
                break;
        }

        return 0;
    }

    void Oversampler::dump(IStateDumper *v) const
    {
        v->write("pCallback", pCallback);
        v->write("fUpBuffer", fUpBuffer);
        v->write("fDownBuffer", fDownBuffer);
        v->write("nUpHead", nUpHead);
        v->write("nMode", nMode);
        v->write("nSampleRate", nSampleRate);
        v->write("nUpdate", nUpdate);
        v->write_object("sFilter", &sFilter);
        v->write("bData", bData);
        v->write("bFilter", bFilter);
    }

} /* namespace lsp */
