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

#include <core/types.h>
#include <dsp/dsp.h>

namespace native
{
    float min(const float *src, size_t count)
    {
        float result = 0.0f;
        if (count > 0)
        {
            result = src[0];
            for (size_t i = 1; i < count; ++i)
            {
                if (src[i] < result)
                    result = src[i];
            }
        }
        return result;
    }

    float max(const float *src, size_t count)
    {
        float result = 0.0f;
        if (count > 0)
        {
            result = src[0];
            for (size_t i = 1; i < count; ++i)
            {
                if (src[i] > result)
                    result = src[i];
            }
        }
        return result;
    }

    float abs_min(const float *src, size_t count)
    {
        float result = 0.0f;
        if (count > 0)
        {
            result = fabsf(src[0]);
            for (size_t i = 1; i < count; ++i)
            {
                float a = fabsf(src[i]);
                if (a < result)
                    result = a;
            }
        }
        return result;
    }

    float abs_max(const float *src, size_t count)
    {
        float result = 0.0f;
        if (count > 0)
        {
            result = fabsf(src[0]);
            for (size_t i = 1; i < count; ++i)
            {
                float a = fabsf(src[i]);
                if (a > result)
                    result = a;
            }
        }
        return result;
    }

    void minmax(const float *src, size_t count, float *min, float *max)
    {
        if (count == 0)
        {
            if (min) *min = 0.0f;
            if (max) *max = 0.0f;
            return;
        }

        float vmin = src[0];
        float vmax = src[0];

        for (size_t i = 1; i < count; ++i)
        {
            float v = src[i];
            if (v < vmin)
                vmin = v;
            if (v > vmax)
                vmax = v;
        }

        if (min) *min = vmin;
        if (max) *max = vmax;
    }

    void abs_minmax(const float *src, size_t count, float *min, float *max)
    {
        if (count == 0)
        {
            if (min) *min = 0.0f;
            if (max) *max = 0.0f;
            return;
        }

        float vmin = fabsf(src[0]);
        float vmax = vmin;

        for (size_t i = 1; i < count; ++i)
        {
            float a = fabsf(src[i]);
            if (a < vmin)
                vmin = a;
            if (a > vmax)
                vmax = a;
        }

        if (min) *min = vmin;
        if (max) *max = vmax;
    }

    size_t min_index(const float *src, size_t count)
    {
        if (count == 0)
            return 0;

        size_t idx = 0;
        float vmin = src[0];

        for (size_t i = 1; i < count; ++i)
        {
            if (src[i] < vmin)
            {
                vmin = src[i];
                idx = i;
            }
        }
        return idx;
    }

    size_t max_index(const float *src, size_t count)
    {
        if (count == 0)
            return 0;

        size_t idx = 0;
        float vmax = src[0];

        for (size_t i = 1; i < count; ++i)
        {
            if (src[i] > vmax)
            {
                vmax = src[i];
                idx = i;
            }
        }
        return idx;
    }

    size_t abs_min_index(const float *src, size_t count)
    {
        if (count == 0)
            return 0;

        size_t idx = 0;
        float vmin = fabsf(src[0]);

        for (size_t i = 1; i < count; ++i)
        {
            float a = fabsf(src[i]);
            if (a < vmin)
            {
                vmin = a;
                idx = i;
            }
        }
        return idx;
    }

    size_t abs_max_index(const float *src, size_t count)
    {
        if (count == 0)
            return 0;

        size_t idx = 0;
        float vmax = fabsf(src[0]);

        for (size_t i = 1; i < count; ++i)
        {
            float a = fabsf(src[i]);
            if (a > vmax)
            {
                vmax = a;
                idx = i;
            }
        }
        return idx;
    }

    void minmax_index(const float *src, size_t count, size_t *min, size_t *max)
    {
        if (count == 0)
        {
            if (min) *min = 0;
            if (max) *max = 0;
            return;
        }

        size_t idx_min = 0;
        size_t idx_max = 0;
        float vmin = src[0];
        float vmax = src[0];

        for (size_t i = 1; i < count; ++i)
        {
            float v = src[i];
            if (v < vmin)
            {
                vmin = v;
                idx_min = i;
            }
            if (v > vmax)
            {
                vmax = v;
                idx_max = i;
            }
        }

        if (min) *min = idx_min;
        if (max) *max = idx_max;
    }

    void abs_minmax_index(const float *src, size_t count, size_t *min, size_t *max)
    {
        if (count == 0)
        {
            if (min) *min = 0;
            if (max) *max = 0;
            return;
        }

        size_t idx_min = 0;
        size_t idx_max = 0;
        float vmin = fabsf(src[0]);
        float vmax = vmin;

        for (size_t i = 1; i < count; ++i)
        {
            float a = fabsf(src[i]);
            if (a < vmin)
            {
                vmin = a;
                idx_min = i;
            }
            if (a > vmax)
            {
                vmax = a;
                idx_max = i;
            }
        }

        if (min) *min = idx_min;
        if (max) *max = idx_max;
    }
} // namespace native