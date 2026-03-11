/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 04 марта 2016 г.
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

#include <stdlib.h>
#include <dsp/dsp.h>
#include <dsp/bits.h>
#include <core/types.h>

#define __DSP_NATIVE_IMPL

#include <dsp/arch/native/copy.h>
#include <dsp/arch/native/mix.h>
#include <dsp/arch/native/fft.h>
#include <dsp/arch/native/pmath.h>
#include <dsp/arch/native/pmath/op_kx.h>
#include <dsp/arch/native/pmath/abs_vv.h>
#include <dsp/arch/native/pmath/minmax.h>
#include <dsp/arch/native/search.h>
#include <dsp/arch/native/filters/static.h>
#include <dsp/arch/native/filters/dynamic.h>
#include <dsp/arch/native/filters/transform.h>
#include <dsp/arch/native/filters/transfer.h>

#undef __DSP_NATIVE_IMPL

namespace native
{
    #define EXPORT1(function)            dsp::function = native::function;

    void dsp_init()
    {
        // Array operations (copy functions)
        EXPORT1(copy);
        EXPORT1(move);
        EXPORT1(fill);
        EXPORT1(fill_zero);
        EXPORT1(fill_one);
        EXPORT1(fill_minus_one);
        EXPORT1(reverse1);
        EXPORT1(reverse2);

        // Pmath operations - op_kx
        EXPORT1(add_k2);
        EXPORT1(sub_k2);
        EXPORT1(rsub_k2);
        EXPORT1(mul_k2);
        EXPORT1(div_k2);
        EXPORT1(rdiv_k2);
        EXPORT1(mod_k2);
        EXPORT1(rmod_k2);
        EXPORT1(add_k3);
        EXPORT1(sub_k3);
        EXPORT1(rsub_k3);
        EXPORT1(mul_k3);
        EXPORT1(div_k3);
        EXPORT1(rdiv_k3);
        EXPORT1(mod_k3);
        EXPORT1(rmod_k3);

        // Pmath operations - abs_vv
        EXPORT1(abs1);
        EXPORT1(abs2);

        // Search operations - minmax
        EXPORT1(min);
        EXPORT1(max);
        EXPORT1(minmax);

        // Mix functions
        EXPORT1(mix2);
        EXPORT1(mix_copy2);
        EXPORT1(mix_add2);
        EXPORT1(mix3);
        EXPORT1(mix_copy3);
        EXPORT1(mix_add3);
        EXPORT1(mix4);
        EXPORT1(mix_copy4);
        EXPORT1(mix_add4);

        // FFT functions
        EXPORT1(direct_fft);
        EXPORT1(packed_direct_fft);
        EXPORT1(reverse_fft);
        EXPORT1(packed_reverse_fft);
        EXPORT1(normalize_fft3);
        EXPORT1(normalize_fft2);
        EXPORT1(center_fft);
        EXPORT1(combine_fft);
        EXPORT1(packed_combine_fft);

        // Normalization functions
        EXPORT1(abs_normalized);
        EXPORT1(normalize);

        // Filter functions - static
        EXPORT1(biquad_process_x1);
        EXPORT1(biquad_process_x2);
        EXPORT1(biquad_process_x4);
        EXPORT1(biquad_process_x8);

        // Filter functions - dynamic
        EXPORT1(dyn_biquad_process_x1);
        EXPORT1(dyn_biquad_process_x2);
        EXPORT1(dyn_biquad_process_x4);
        EXPORT1(dyn_biquad_process_x8);

        // Filter transfer functions
        EXPORT1(filter_transfer_calc_ri);
        EXPORT1(filter_transfer_apply_ri);
        EXPORT1(filter_transfer_calc_pc);
        EXPORT1(filter_transfer_apply_pc);

        // Filter transform functions - bilinear
        EXPORT1(bilinear_transform_x1);
        EXPORT1(bilinear_transform_x2);
        EXPORT1(bilinear_transform_x4);
        EXPORT1(bilinear_transform_x8);

        // Filter transform functions - matched Z
        EXPORT1(matched_transform_x1);
        EXPORT1(matched_transform_x2);
        EXPORT1(matched_transform_x4);
        EXPORT1(matched_transform_x8);
    }

    #undef EXPORT1
}
