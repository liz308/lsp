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

#ifndef DSP_DSP_H_
#define DSP_DSP_H_

#include <common/types.h>
#include <core/debug.h>

#include <stddef.h>
#include <math.h>
#include <string.h>

#define __DSP_DSP_DEFS

// Minimal includes for array operations
#include <dsp/common/copy.h>
#include <dsp/common/mix.h>
#include <dsp/common/fft.h>
#include <dsp/common/filters.h>
#include <dsp/common/misc.h>

// Include pmath operations
#include <dsp/common/pmath/op_kx.h>
#include <dsp/common/pmath/abs_vv.h>
#include <dsp/common/pmath/minmax.h>
#include <dsp/common/search/minmax.h>

#undef __DSP_DSP_DEFS

namespace dsp
{
    /** Initialize DSP
     */
    void init();
}

#endif /* DSP_DSP_H_ */