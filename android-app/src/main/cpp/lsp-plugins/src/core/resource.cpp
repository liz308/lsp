/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 14 июл. 2019 г.
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

#include <core/resource.h>

namespace lsp
{
    namespace resource
    {
        const resource_t *get(const char *id, resource_type_t type)
        {
            return NULL;
        }

        const resource_t *all()
        {
            return NULL;
        }

        const char *fetch_dstring(const void **ptr)
        {
            return NULL;
        }

        float fetch_dfloat(const void **ptr)
        {
            return 0.0f;
        }

        uint64_t fetch_number(const void **ptr)
        {
            return 0;
        }

        uint8_t fetch_byte(const void **ptr)
        {
            return 0;
        }

        uint8_t get_byte(const void **ptr)
        {
            return 0;
        }

        ssize_t fetch_bytes(void *dst, const void **ptr, size_t count)
        {
            return -1;
        }

        ssize_t skip_bytes(const void **ptr, size_t count)
        {
            return -1;
        }
    }
}
