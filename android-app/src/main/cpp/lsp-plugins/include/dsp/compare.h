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

#ifndef LSP_PLUGINS_DSP_COMPARE_H_
#define LSP_PLUGINS_DSP_COMPARE_H_

#include <stddef.h>

namespace lsp
{
    namespace dsp
    {
        /**
         * Calculate min { src }
         * @param src source vector
         * @param count number of elements
         * @return minimum value
         */
        float compare_min(const float *src, size_t count);

        /**
         * Calculate max { src }
         * @param src source vector
         * @param count number of elements
         * @return maximum value
         */
        float compare_max(const float *src, size_t count);

        /**
         * Get absolute minimum: result = min { abs(src[i]) }
         * @param src source array
         * @param count number of elements
         * @return result
         */
        float compare_abs_min(const float *src, size_t count);

        /**
         * Get absolute maximum: result = max { abs(src[i]) }
         * @param src source array
         * @param count number of elements
         * @return result
         */
        float compare_abs_max(const float *src, size_t count);

        /**
         * Calculate min { src }, max { src }
         * @param src source vector
         * @param count number of elements
         * @param min pointer to store minimum value
         * @param max pointer to store maximum value
         */
        void compare_minmax(const float *src, size_t count, float *min, float *max);

        /**
         * Calculate min { abs(src) }, max { abs(src) }
         * @param src source vector
         * @param count number of elements
         * @param min pointer to store absolute minimum value
         * @param max pointer to store absolute maximum value
         */
        void compare_abs_minmax(const float *src, size_t count, float *min, float *max);

        /**
         * Calculate index of minimum value
         * @param src source vector
         * @param count number of elements
         * @return minimum value index
         */
        size_t compare_min_index(const float *src, size_t count);

        /**
         * Calculate index of maximum value
         * @param src source vector
         * @param count number of elements
         * @return maximum value index
         */
        size_t compare_max_index(const float *src, size_t count);

        /**
         * Calculate index of absolute minimum value
         * @param src source vector
         * @param count number of elements
         * @return absolute minimum value index
         */
        size_t compare_abs_min_index(const float *src, size_t count);

        /**
         * Calculate index of absolute maximum value
         * @param src source vector
         * @param count number of elements
         * @return absolute maximum value index
         */
        size_t compare_abs_max_index(const float *src, size_t count);

        /**
         * Calculate indices of minimum and maximum values
         * @param src source vector
         * @param count number of elements
         * @param min pointer to store minimum value index
         * @param max pointer to store maximum value index
         */
        void compare_minmax_index(const float *src, size_t count, size_t *min, size_t *max);

        /**
         * Calculate indices of absolute minimum and maximum values
         * @param src source vector
         * @param count number of elements
         * @param min pointer to store absolute minimum value index
         * @param max pointer to store absolute maximum value index
         */
        void compare_abs_minmax_index(const float *src, size_t count, size_t *min, size_t *max);
    } // namespace dsp
} // namespace lsp

#endif /* LSP_PLUGINS_DSP_COMPARE_H_ */ilary 4096 Mar 10 06:06 .
drwxrwxr-x 4 hilary hilary 4096 Mar 10 06:07 ..
-rw-rw-r-- 1 hilary hilary 2606 Mar 10 06:35 bits.cpp
-rw-rw-r-- 1 hilary hilary 6429 Mar 10 05:30 compare.cpp
-rw-rw-r-- 1 hilary hilary 2220 Mar 10 06:34 dsp.cpp
-rw-rw-r-- 1 hilary hilary    0 Mar 10 05:39 matrix.cpp
-rw-rw-r-- 1 hilary hilary 1130 Mar 10 06:34 native.cpp
Accepted creation of
￼
alloc.cpp
￼
￼
Command
￼
￼
￼
￼
find android-app/src/main/cpp -name "dsp" -type d
Credits used: 0
Elapsedilary 4096 Mar 10 06:06 .
drwxrwxr-x 4 hilary hilary 4096 Mar 10 06:07 ..
-rw-rw-r-- 1 hilary hilary 2606 Mar 10 06:35 bits.cpp
-rw-rw-r-- 1 hilary hilary 6429 Mar 10 05:30 compare.cpp
-rw-rw-r-- 1 hilary hilary 2220 Mar 10 06:34 dsp.cpp
-rw-rw-r-- 1 hilary hilary    0 Mar 10 05:39 matrix.cpp
-rw-rw-r-- 1 hilary hilary 1130 Mar 10 06:34 native.cpp
Accepted creation of
￼
alloc.cpp
￼
￼
Command
￼
￼
￼
￼
find android-app/src/main/cpp -name "dsp" -type d
Credits used: 0
Elapsedilary 4096 Mar 10 06:06 .
drwxrwxr-x 4 hilary hilary 4096 Mar 10 06:07 ..
-rw-rw-r-- 1 hilary hilary 2606 Mar 10 06:35 bits.cpp
-rw-rw-r-- 1 hilary hilary 6429 Mar 10 05:30 compare.cpp
-rw-rw-r-- 1 hilary hilary 2220 Mar 10 06:34 dsp.cpp
-rw-rw-r-- 1 hilary hilary    0 Mar 10 05:39 matrix.cpp
-rw-rw-r-- 1 hilary hilary 1130 Mar 10 06:34 native.cpp
Accepted creation of
￼
alloc.cpp
￼
￼
Command
￼
￼
￼
￼
find android-app/src/main/cpp -name "dsp" -type d
Credits used: 0
Elapsed