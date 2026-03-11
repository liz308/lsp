/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 02 окт. 2015 г.
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

#include <math.h>
#include <core/types.h>
#include <core/debug.h>
#include <dsp/dsp.h>

namespace native
{
    extern void dsp_init();
}

namespace dsp
{
    // Array operations (copy functions)
    void    (* copy)(float *dst, const float *src, size_t count) = NULL;
    void    (* move)(float *dst, const float *src, size_t count) = NULL;
    void    (* fill)(float *dst, float value, size_t count) = NULL;
    void    (* fill_zero)(float *dst, size_t count) = NULL;
    void    (* fill_one)(float *dst, size_t count) = NULL;
    void    (* fill_minus_one)(float *dst, size_t count) = NULL;
    void    (* reverse1)(float *dst, size_t count) = NULL;
    void    (* reverse2)(float *dst, const float *src, size_t count) = NULL;

    // Pmath operations - op_kx
    void    (* add_k2)(float *dst, float k, size_t count) = NULL;
    void    (* sub_k2)(float *dst, float k, size_t count) = NULL;
    void    (* rsub_k2)(float *dst, float k, size_t count) = NULL;
    void    (* mul_k2)(float *dst, float k, size_t count) = NULL;
    void    (* div_k2)(float *dst, float k, size_t count) = NULL;
    void    (* rdiv_k2)(float *dst, float k, size_t count) = NULL;
    void    (* mod_k2)(float *dst, float k, size_t count) = NULL;
    void    (* rmod_k2)(float *dst, float k, size_t count) = NULL;
    void    (* add_k3)(float *dst, const float *src, float k, size_t count) = NULL;
    void    (* sub_k3)(float *dst, const float *src, float k, size_t count) = NULL;
    void    (* rsub_k3)(float *dst, const float *src, float k, size_t count) = NULL;
    void    (* mul_k3)(float *dst, const float *src, float k, size_t count) = NULL;
    void    (* div_k3)(float *dst, const float *src, float k, size_t count) = NULL;
    void    (* rdiv_k3)(float *dst, const float *src, float k, size_t count) = NULL;
    void    (* mod_k3)(float *dst, const float *src, float k, size_t count) = NULL;
    void    (* rmod_k3)(float *dst, const float *src, float k, size_t count) = NULL;

    // Pmath operations - abs_vv
    void    (* abs1)(float *dst, size_t count) = NULL;
    void    (* abs2)(float *dst, const float *src, size_t count) = NULL;

    // Search operations - minmax
    float   (* min)(const float *src, size_t count) = NULL;
    float   (* max)(const float *src, size_t count) = NULL;
    void    (* minmax)(const float *src, size_t count, float *min, float *max) = NULL;

    // Mix functions
    void    (* mix2)(float *dst, const float *src, float k1, float k2, size_t count) = NULL;
    void    (* mix_copy2)(float *dst, const float *src1, const float *src2, float k1, float k2, size_t count) = NULL;
    void    (* mix_add2)(float *dst, const float *src1, const float *src2, float k1, float k2, size_t count) = NULL;
    void    (* mix3)(float *dst, const float *src1, const float *src2, float k1, float k2, float k3, size_t count) = NULL;
    void    (* mix_copy3)(float *dst, const float *src1, const float *src2, const float *src3, float k1, float k2, float k3, size_t count) = NULL;
    void    (* mix_add3)(float *dst, const float *src1, const float *src2, const float *src3, float k1, float k2, float k3, size_t count) = NULL;
    void    (* mix4)(float *dst, const float *src1, const float *src2, const float *src3, float k1, float k2, float k3, float k4, size_t count) = NULL;
    void    (* mix_copy4)(float *dst, const float *src1, const float *src2, const float *src3, const float *src4, float k1, float k2, float k3, float k4, size_t count) = NULL;
    void    (* mix_add4)(float *dst, const float *src1, const float *src2, const float *src3, const float *src4, float k1, float k2, float k3, float k4, size_t count) = NULL;

    // FFT functions
    void    (* direct_fft)(float *dst_re, float *dst_im, const float *src_re, const float *src_im, size_t rank) = NULL;
    void    (* packed_direct_fft)(float *dst, const float *src, size_t rank) = NULL;
    void    (* reverse_fft)(float *dst_re, float *dst_im, const float *src_re, const float *src_im, size_t rank) = NULL;
    void    (* packed_reverse_fft)(float *dst, const float *src, size_t rank) = NULL;
    void    (* normalize_fft3)(float *dst_re, float *dst_im, const float *src_re, const float *src_im, size_t rank) = NULL;
    void    (* normalize_fft2)(float *re, float *im, size_t rank) = NULL;
    void    (* center_fft)(float *dst_re, float *dst_im, const float *src_re, const float *src_im, size_t rank) = NULL;
    void    (* combine_fft)(float *dst_re, float *dst_im, const float *src_re, const float *src_im, size_t rank) = NULL;
    void    (* packed_combine_fft)(float *dst, const float *src, size_t rank) = NULL;

    // Normalization functions
    void    (* abs_normalized)(float *dst, const float *src, size_t count) = NULL;
    void    (* normalize)(float *dst, const float *src, size_t count) = NULL;

    // Filter functions - static
    void    (* biquad_process_x1)(float *dst, const float *src, size_t count, biquad_t *f) = NULL;
    void    (* biquad_process_x2)(float *dst, const float *src, size_t count, biquad_t *f) = NULL;
    void    (* biquad_process_x4)(float *dst, const float *src, size_t count, biquad_t *f) = NULL;
    void    (* biquad_process_x8)(float *dst, const float *src, size_t count, biquad_t *f) = NULL;

    // Filter functions - dynamic
    void    (* dyn_biquad_process_x1)(float *dst, const float *src, float *d, size_t count, const biquad_x1_t *f) = NULL;
    void    (* dyn_biquad_process_x2)(float *dst, const float *src, float *d, size_t count, const biquad_x2_t *f) = NULL;
    void    (* dyn_biquad_process_x4)(float *dst, const float *src, float *d, size_t count, const biquad_x4_t *f) = NULL;
    void    (* dyn_biquad_process_x8)(float *dst, const float *src, float *d, size_t count, const biquad_x8_t *f) = NULL;

    // Filter transfer functions
    void    (* filter_transfer_calc_ri)(float *re, float *im, const f_cascade_t *c, const float *freq, size_t count) = NULL;
    void    (* filter_transfer_apply_ri)(float *re, float *im, const f_cascade_t *c, const float *freq, size_t count) = NULL;
    void    (* filter_transfer_calc_pc)(float *dst, const f_cascade_t *c, const float *freq, size_t count) = NULL;
    void    (* filter_transfer_apply_pc)(float *dst, const f_cascade_t *c, const float *freq, size_t count) = NULL;

    // Filter transform functions - bilinear
    void    (* bilinear_transform_x1)(biquad_x1_t *bf, const f_cascade_t *bc, float kf, size_t count) = NULL;
    void    (* bilinear_transform_x2)(biquad_x2_t *bf, const f_cascade_t *bc, float kf, size_t count) = NULL;
    void    (* bilinear_transform_x4)(biquad_x4_t *bf, const f_cascade_t *bc, float kf, size_t count) = NULL;
    void    (* bilinear_transform_x8)(biquad_x8_t *bf, const f_cascade_t *bc, float kf, size_t count) = NULL;

    // Filter transform functions - matched Z
    void    (* matched_transform_x1)(biquad_x1_t *bf, f_cascade_t *bc, float kf, float td, size_t count) = NULL;
    void    (* matched_transform_x2)(biquad_x2_t *bf, f_cascade_t *bc, float kf, float td, size_t count) = NULL;
    void    (* matched_transform_x4)(biquad_x4_t *bf, f_cascade_t *bc, float kf, float td, size_t count) = NULL;
    void    (* matched_transform_x8)(biquad_x8_t *bf, f_cascade_t *bc, float kf, float td, size_t count) = NULL;

    void init()
    {
        native::dsp_init();
    }
}
