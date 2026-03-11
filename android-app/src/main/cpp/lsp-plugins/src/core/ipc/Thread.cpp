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

#include <core/types.h>
#include <dsp/atomic.h>
#include <core/ipc/Thread.h>
#include <time.h>
#include <errno.h>
#include <unistd.h>
#include <pthread.h>

namespace lsp
{
    namespace ipc
    {
        __thread Thread *Thread::pThis = NULL;

        Thread::Thread()
        {
            enState             = TS_CREATED;
            nResult             = STATUS_OK;
            bCancelled          = false;
            hThread             = 0;
            sBinding.proc       = NULL;
            sBinding.arg        = NULL;
            sBinding.runnable   = NULL;
        }
        
        Thread::Thread(thread_proc_t proc)
        {
            enState             = TS_CREATED;
            nResult             = STATUS_OK;
            bCancelled          = false;
            hThread             = 0;
            sBinding.proc       = proc;
            sBinding.arg        = NULL;
            sBinding.runnable   = NULL;
        }

        Thread::Thread(thread_proc_t proc, void *arg)
        {
            enState             = TS_CREATED;
            nResult             = STATUS_OK;
            bCancelled          = false;
            hThread             = 0;
            sBinding.proc       = proc;
            sBinding.runnable   = NULL;
            sBinding.arg        = arg;
        }

        Thread::Thread(IRunnable *runnable)
        {
            enState             = TS_CREATED;
            nResult             = STATUS_OK;
            bCancelled          = false;
            hThread             = 0;
            sBinding.proc       = NULL;
            sBinding.arg        = NULL;
            sBinding.runnable   = runnable;
        }

        Thread::~Thread()
        {
            if (hThread != 0)
                pthread_detach(hThread);
            hThread             = 0;
        }

        status_t Thread::run()
        {
            if (sBinding.proc != NULL)
                return sBinding.proc(sBinding.arg);
            else if (sBinding.runnable != NULL)
                return sBinding.runnable->run();
            return STATUS_OK;
        }

        status_t Thread::start()
        {
            if (enState != TS_CREATED)
                return STATUS_BAD_STATE;

            enState = TS_RUNNING;
            return STATUS_OK;
        }

        status_t Thread::join()
        {
            if (hThread == 0)
                return STATUS_BAD_STATE;

            if (pthread_join(hThread, NULL) != 0)
                return STATUS_IO_ERROR;

            hThread = 0;
            enState = TS_FINISHED;
            return STATUS_OK;
        }

        bool Thread::is_running() const
        {
            return enState == TS_RUNNING;
        }

        bool Thread::is_finished() const
        {
            return enState == TS_FINISHED;
        }

        Thread *Thread::current()
        {
            return pThis;
        }

    } /* namespace ipc */
} /* namespace lsp */
