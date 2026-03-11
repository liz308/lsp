/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 13 июл. 2019 г.
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

#include <core/parse.h>
#include <math.h>
#include <errno.h>
#include <stdlib.h>
#include <locale.h>

namespace lsp
{
    bool parse_float(const char *variable, float *res)
    {
        errno = 0;
        char *end   = NULL;
        float value = strtof(variable, &end);

        bool success = (errno == 0);
        if ((end != NULL) && (success))
        {
            while ((*end) == ' ')
                ++ end;
            if (((end[0] == 'd') || (end[0] == 'D')) &&
                ((end[1] == 'b') || (end[1] == 'B')))
                value   = expf(value * M_LN10 * 0.05f);
        }

        if (res != NULL)
            *res        = value;
        return success;
    }

    bool parse_double(const char *variable, double *res)
    {
        errno = 0;
        char *end       = NULL;
        double value    = strtod(variable, &end);

        bool success    = (errno == 0);
        if ((end != NULL) && (success))
        {
            while ((*end) == ' ')
                ++ end;
            if (((end[0] == 'd') || (end[0] == 'D')) &&
                ((end[1] == 'b') || (end[1] == 'B')))
                value   = exp(value * M_LN10 * 0.05);
        }

        if (res != NULL)
            *res        = value;
        return success;
    }
}
