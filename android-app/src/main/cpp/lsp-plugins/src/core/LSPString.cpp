/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 30 авг. 2017 г.
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
#include <core/stdlib/stdio.h>
#include <core/stdlib/string.h>
#include <core/LSPString.h>

#include <stdlib.h>
#include <errno.h>
#include <wctype.h>
#include <stdarg.h>
#include <string.h>

#define GRANULARITY     0x20

namespace lsp
{
    static bool is_space(lsp_wchar_t c)
    {
        switch (c)
        {
            case ' ':
            case '\t':
            case '\n':
            case '\r':
            case '\v':
                return true;
            default:
                return false;
        }
    }

    LSPString::LSPString()
    {
        nLength     = 0;
        nCapacity   = 0;
        pData       = NULL;
        pTemp       = NULL;
    }

    LSPString::~LSPString()
    {
        truncate();
    }

    inline size_t LSPString::xlen(const lsp_wchar_t *s)
    {
        const lsp_wchar_t *p = s;
        while (*p != '\0')
            ++p;
        return p - s;
    }

    void LSPString::drop_temp()
    {
        if (pTemp == NULL)
            return;
        if (pTemp->pData != NULL)
            ::free(pTemp->pData);
        ::free(pTemp);
        pTemp = NULL;
    }

    void LSPString::clear()
    {
        drop_temp();
        nLength     = 0;
    }

    void LSPString::truncate()
    {
        drop_temp();
        nLength     = 0;
        nCapacity   = 0;
        if (pData == NULL)
            return;
        ::free(pData);
        pData = NULL;
    }

    bool LSPString::truncate(size_t size)
    {
        drop_temp();
        if (size > nCapacity)
            return true;
        if (nLength > size)
            nLength = size;
        lsp_wchar_t *v = (lsp_wchar_t *)::realloc(pData, size * sizeof(lsp_wchar_t));
        if ((v == NULL) && (size > 0))
            return false;
        pData       = (size > 0) ? v : NULL;
        nCapacity   = size;
        return true;
    }

    size_t LSPString::set_length(size_t length)
    {
        if (nLength <= length)
            return length;
        drop_temp();
        return nLength = length;
    }

    bool LSPString::size_reserve(size_t size)
    {
        if (size > 0)
        {
            lsp_wchar_t *v = (lsp_wchar_t *)::realloc(pData, size * sizeof(lsp_wchar_t));
            if (v == NULL)
                return false;
            pData   = v;
        }
        else if (pData != NULL)
        {
            ::free(pData);
            pData   = NULL;
        }
        nCapacity   = size;
        return true;
    }

    inline bool LSPString::cap_reserve(size_t size)
    {
        size_t ncap = (size + (GRANULARITY-1)) & (~(GRANULARITY-1));
        return (ncap > nCapacity) ? size_reserve(ncap) : true;
    }

    inline bool LSPString::cap_grow(size_t delta)
    {
        size_t avail = nCapacity - nLength;
        if (delta <= avail)
            return true;
        avail = nCapacity >> 1;
        if (avail < delta)
            avail = delta;
        return size_reserve(nCapacity + ((avail + (GRANULARITY-1)) & (~(GRANULARITY-1))));
    }

    void LSPString::reduce()
    {
        drop_temp();
        if (nCapacity <= nLength)
            return;
        lsp_wchar_t *v = (lsp_wchar_t *)::realloc(pData, nLength * sizeof(lsp_wchar_t));
        if ((v == NULL) && (nLength > 0))
            return;
        pData       = (nLength > 0) ? v : NULL;
        nCapacity   = nLength;
    }

    void LSPString::trim()
    {
        if ((pData == NULL) || (nLength <= 0))
            return;

        lsp_wchar_t *p = &pData[nLength];
        while (nLength > 0)
        {
            if (!is_space(*(--p)))
                break;
            nLength--;
        }
        if (nLength <= 0)
            return;

        p = pData;
        while (true)
        {
            if (!is_space(*p))
                break;
            p++;
        }
        if (p > pData)
            nLength -= (p - pData);
        if (nLength <= 0)
            return;

        ::memmove(pData, p, nLength * sizeof(lsp_wchar_t));
    }

    void LSPString::swap(LSPString *src)
    {
        size_t len      = src->nLength;
        size_t cap      = src->nCapacity;
        lsp_wchar_t *c  = src->pData;

        src->nLength    = nLength;
        src->nCapacity  = nCapacity;
        src->pData      = pData;

        nLength         = len;
        nCapacity       = cap;
        pData           = c;
    }

    void LSPString::take(LSPString *src)
    {
        drop_temp();
        if (pData != NULL)
            ::free(pData);

        nLength         = src->nLength;
        nCapacity       = src->nCapacity;
        pData           = src->pData;

        src->nLength    = 0;
        src->nCapacity  = 0;
        src->pData      = NULL;
    }

    LSPString *LSPString::copy() const
    {
        LSPString *s = new LSPString();
        if (s == NULL)
            return s;

        s->nLength      = nLength;
        s->nCapacity    = nLength;
        if (s->nLength > 0)
        {
            s->pData        = (lsp_wchar_t *)::malloc(nLength * sizeof(lsp_wchar_t));
            if (s->pData == NULL)
            {
                delete s;
                return NULL;
            }
            ::memcpy(s->pData, pData, nLength * sizeof(lsp_wchar_t));
        }
        else
            s->pData        = NULL;

        return s;
    }

    LSPString *LSPString::release()
    {
        LSPString *str = new LSPString();
        if (str != NULL)
            str->swap(this);
        return str;
    }

    lsp_wchar_t LSPString::at(ssize_t index) const
    {
        if (index < 0)
        {
            if ((index += nLength) < 0)
                return 0;
        }
        else if (size_t(index) >= nLength)
            return 0;
        return pData[index];
    }

    lsp_wchar_t LSPString::first() const
    {
        return (nLength > 0) ? pData[0] : 0;
    }

    lsp_wchar_t LSPString::last() const
    {
        return (nLength > 0) ? pData[nLength-1] : 0;
    }

    bool LSPString::set(lsp_wchar_t ch)
    {
        drop_temp();
        if (nCapacity == 0)
        {
            lsp_wchar_t *v = (lsp_wchar_t *)::malloc(GRANULARITY * sizeof(lsp_wchar_t));
            if (v == NULL)
                return false;
            v[0]        = ch;
            pData       = v;
            nCapacity   = GRANULARITY;
        }
        else
            pData[0]    = ch;
        nLength     = 1;
        return true;
    }

    bool LSPString::set(const lsp_wchar_t *arr)
    {
        return set(arr, xlen(arr));
    }

    bool LSPString::set(const lsp_wchar_t *arr, size_t n)
    {
        drop_temp();
        if (!cap_reserve(n))
            return false;
        ::memcpy(pData, arr, n * sizeof(lsp_wchar_t));
        nLength     = n;
        return true;
    }

    bool LSPString::set(const LSPString *src)
    {
        if (src == this)
            return true;
        drop_temp();
        if (!cap_reserve(src->nLength))
            return false;
        if (src->nLength > 0)
            ::memcpy(pData, src->pData, src->nLength * sizeof(lsp_wchar_t));
        nLength     = src->nLength;
        return true;
    }

    bool LSPString::append(lsp_wchar_t ch)
    {
        if (!cap_grow(1))
            return false;
        pData[nLength++] = ch;
        return true;
    }

    bool LSPString::append(const lsp_wchar_t *arr, size_t n)
    {
        if (!cap_grow(n))
            return false;
        ::memcpy(&pData[nLength], arr, n * sizeof(lsp_wchar_t));
        nLength += n;
        return true;
    }

    bool LSPString::append(const LSPString *src)
    {
        if (src->nLength <= 0)
            return true;
        if (!cap_grow(src->nLength))
            return false;
        ::memcpy(&pData[nLength], src->pData, src->nLength * sizeof(lsp_wchar_t));
        nLength += src->nLength;
        return true;
    }

    bool LSPString::prepend(lsp_wchar_t ch)
    {
        if (!cap_grow(1))
            return false;
        if (nLength > 0)
            ::memmove(&pData[1], pData, nLength * sizeof(lsp_wchar_t));
        pData[0]    = ch;
        nLength     ++;
        return true;
    }

    bool LSPString::prepend(const lsp_wchar_t *arr, size_t n)
    {
        if (n <= 0)
            return true;
        if (!cap_grow(n))
            return false;
        if (nLength > 0)
            ::memmove(&pData[n], pData, nLength * sizeof(lsp_wchar_t));
        ::memcpy(pData, arr, n * sizeof(lsp_wchar_t));
        nLength += n;
        return true;
    }

    bool LSPString::prepend(const LSPString *src)
    {
        if (src->nLength <= 0)
            return true;
        if (!cap_grow(src->nLength))
            return false;
        if (nLength > 0)
            ::memmove(&pData[src->nLength], pData, nLength * sizeof(lsp_wchar_t));
        ::memcpy(pData, src->pData, src->nLength * sizeof(lsp_wchar_t));
        nLength += src->nLength;
        return true;
    }

    bool LSPString::ends_with(lsp_wchar_t ch) const
    {
        return (nLength > 0) ? pData[nLength-1] == ch : false;
    }

    bool LSPString::starts_with(lsp_wchar_t ch) const
    {
        return (nLength > 0) ? pData[0] == ch : false;
    }

    bool LSPString::remove()
    {
        drop_temp();
        nLength = 0;
        return true;
    }

    bool LSPString::remove_last()
    {
        if (nLength <= 0)
            return false;
        --nLength;
        return true;
    }

    void LSPString::reverse()
    {
        drop_temp();
        size_t n = (nLength >> 1);
        lsp_wchar_t *h = pData, *t = &pData[nLength];
        while (n--)
        {
            lsp_wchar_t c = *h;
            *(h++)  = *(--t);
            *t      = c;
        }
    }

    size_t LSPString::length() const
    {
        return nLength;
    }

    const lsp_wchar_t *LSPString::get_native() const
    {
        return pData;
    }

    lsp_wchar_t *LSPString::get_native()
    {
        return pData;
    }

    bool LSPString::is_empty() const
    {
        return nLength == 0;
    }

    ssize_t LSPString::index_of(const LSPString *str) const
    {
        if (str->nLength <= 0)
            return 0;
        if (nLength < str->nLength)
            return -1;

        for (size_t i = 0; i <= nLength - str->nLength; ++i)
        {
            if (::memcmp(&pData[i], str->pData, str->nLength * sizeof(lsp_wchar_t)) == 0)
                return i;
        }
        return -1;
    }

    ssize_t LSPString::rindex_of(const LSPString *str) const
    {
        if (str->nLength <= 0)
            return nLength;
        if (nLength < str->nLength)
            return -1;

        for (ssize_t i = nLength - str->nLength; i >= 0; --i)
        {
            if (::memcmp(&pData[i], str->pData, str->nLength * sizeof(lsp_wchar_t)) == 0)
                return i;
        }
        return -1;
    }

    bool LSPString::equals(const LSPString *src) const
    {
        if (nLength != src->nLength)
            return false;
        if (nLength == 0)
            return true;
        return ::memcmp(pData, src->pData, nLength * sizeof(lsp_wchar_t)) == 0;
    }

    int LSPString::compare(const LSPString *src) const
    {
        size_t minlen = (nLength < src->nLength) ? nLength : src->nLength;
        if (minlen > 0)
        {
            int cmp = ::memcmp(pData, src->pData, minlen * sizeof(lsp_wchar_t));
            if (cmp != 0)
                return cmp;
        }
        if (nLength < src->nLength)
            return -1;
        if (nLength > src->nLength)
            return 1;
        return 0;
    }
}
