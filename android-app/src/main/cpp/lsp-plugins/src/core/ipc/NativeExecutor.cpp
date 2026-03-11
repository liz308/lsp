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

#include <core/debug.h>
#include <core/ipc/NativeExecutor.h>

namespace lsp
{
    namespace ipc
    {
        NativeExecutor::NativeExecutor():
            hThread(execute, this)
        {
            pHead       = NULL;
            pTail       = NULL;
            atomic_init(nLock);
        }

        NativeExecutor::~NativeExecutor()
        {
        }

        status_t NativeExecutor::start()
        {
            return hThread.start();
        }

        bool NativeExecutor::submit(ITask *task)
        {
            if (!task->idle())
                return false;

            if (!atomic_trylock(nLock))
                return false;

            change_task_state(task, ITask::TS_SUBMITTED);

            if (pTail != NULL)
                link_task(pTail, task);
            else
                pHead   = task;
            pTail   = task;

            atomic_unlock(nLock);
            return true;
        }

        void NativeExecutor::shutdown()
        {
            while (true)
            {
                if (atomic_trylock(nLock))
                {
                    if (pHead == NULL)
                        break;
                    atomic_unlock(nLock);
                }

                ipc::Thread::sleep(100);
            }

            hThread.cancel();
            hThread.join();
        }

        void NativeExecutor::run()
        {
            while (!ipc::Thread::is_cancelled())
            {
                while (!atomic_trylock(nLock))
                {
                    if (ipc::Thread::sleep(100) == STATUS_CANCELLED)
                        return;
                }

                ITask  *task    = pHead;
                if (task == NULL)
                {
                    atomic_unlock(nLock);

                    if (ipc::Thread::sleep(100) == STATUS_CANCELLED)
                        return;
                }
                else
                {
                    pHead           = next_task(pHead);
                    if (pHead == NULL)
                        pTail           = NULL;

                    atomic_unlock(nLock);

                    run_task(task);
                }
            }
        }

        status_t NativeExecutor::execute(void *params)
        {
            NativeExecutor *_this = reinterpret_cast<NativeExecutor *>(params);
            _this->run();
            return STATUS_OK;
        }
    } /* namespace ipc */
} /* namespace lsp */
