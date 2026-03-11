/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 27 янв. 2016 г.
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
#include <core/IWrapper.h>
#include <core/plugin.h>

namespace lsp
{
    IWrapper::IWrapper(plugin_t *plugin)
    {
        pPlugin     = plugin;
    }

    IWrapper::~IWrapper()
    {
    }

    ipc::IExecutor *IWrapper::get_executor()
    {
        return NULL;
    }

    void IWrapper::query_display_draw()
    {
    }

    const position_t *IWrapper::position()
    {
        return NULL;
    }

    ICanvas *IWrapper::create_canvas(ICanvas *&cv, size_t width, size_t height)
    {
        return NULL;
    }

    KVTStorage *IWrapper::kvt_lock()
    {
        return NULL;
    }

    KVTStorage *IWrapper::kvt_trylock()
    {
        return NULL;
    }

    bool IWrapper::kvt_release()
    {
        return false;
    }

    void IWrapper::state_changed()
    {
    }

    void IWrapper::dump_plugin_state()
    {
    }

} /* namespace lsp */
