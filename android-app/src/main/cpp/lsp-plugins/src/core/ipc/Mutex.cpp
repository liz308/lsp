/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 25 февр. 2019 г.
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

#include <core/ipc/Mutex.h>
#include <errno.h>
#include <pthread.h>

namespace lsp
{
    namespace ipc
    {
        Mutex::Mutex()
        {
            pthread_mutexattr_t attr;
            pthread_mutexattr_init(&attr);
            pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);
            pthread_mutex_init(&sMutex, &attr);
            pthread_mutexattr_destroy(&attr);
        }

        Mutex::~Mutex()
        {
            pthread_mutex_destroy(&sMutex);
        }

        bool Mutex::lock() const
        {
            return pthread_mutex_lock(&sMutex) == 0;
        }

        bool Mutex::try_lock() const
        {
            return pthread_mutex_trylock(&sMutex) == 0;
        }

        bool Mutex::unlock() const
        {
            return pthread_mutex_unlock(&sMutex) == 0;
        }

    } /* namespace ipc */
} /* namespace lsp */
