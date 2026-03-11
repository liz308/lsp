/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 21 дек. 2019 г.
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

#include <core/files/url.h>

namespace lsp
{
    static inline int decode_hex(lsp_wchar_t c)
    {
        if ((c >= '0') && (c <= '9'))
            return c - '0';
        else if ((c >= 'a') && (c <= 'f'))
            return c - 'a' + 10;
        else if ((c >= 'A') && (c <= 'F'))
            return c - 'A' + 10;
        return -1;
    }

    status_t url_decode(LSPString *dst, const LSPString *src)
    {
        return url_decode(dst, src, 0, src->length());
    }

    status_t url_decode(LSPString *dst, const LSPString *src, size_t first)
    {
        return url_decode(dst, src, first, src->length());
    }

    status_t url_decode(LSPString *dst, const LSPString *src, size_t first, size_t last)
    {
        if ((dst == NULL) || (src == NULL))
            return STATUS_BAD_ARGUMENTS;

        while (first < last)
        {
            lsp_wchar_t c = src->at(first);
            if (c == '%')
            {
                if ((last - first) < 3)
                    return STATUS_CORRUPTED;

                int code    = decode_hex(src->at(++first)) << 4;
                code       |= decode_hex(src->at(++first));
                if (code < 0)
                    return STATUS_CORRUPTED;

                if (!dst->append(static_cast<lsp_wchar_t>(code)))
                    return STATUS_NO_MEM;
            }
            else if (!dst->append(c))
                return STATUS_NO_MEM;

            ++first;
        }

        return STATUS_OK;
    }
}
