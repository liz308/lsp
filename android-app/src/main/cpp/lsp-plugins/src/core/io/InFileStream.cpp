/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 13 мар. 2019 г.
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

#include <core/io/NativeFile.h>
#include <core/io/StdioFile.h>
#include <core/io/InFileStream.h>

namespace lsp
{
    namespace io
    {
        InFileStream::InFileStream()
        {
            pFD         = NULL;
            nWrapFlags  = 0;
        }
        
        InFileStream::~InFileStream()
        {
            if (pFD != NULL)
            {
                if (nWrapFlags & WRAP_CLOSE)
                    pFD->close();
                if (nWrapFlags & WRAP_DELETE)
                    delete pFD;
                pFD         = NULL;
            }
            nWrapFlags  = 0;
        }

        status_t InFileStream::close()
        {
            status_t res = STATUS_OK;

            if (pFD != NULL)
            {
                if (nWrapFlags & WRAP_CLOSE)
                    res = pFD->close();
                if (nWrapFlags & WRAP_DELETE)
                    delete pFD;
                pFD         = NULL;
            }
            nWrapFlags  = 0;

            return set_error(res);
        }

        status_t InFileStream::wrap(File *fd, size_t flags)
        {
            if (pFD != NULL)
                return set_error(STATUS_BAD_STATE);
            else if (fd == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            pFD         = fd;
            nWrapFlags  = flags;

            return set_error(STATUS_OK);
        }

        status_t InFileStream::open(const char *path)
        {
            if (pFD != NULL)
                return set_error(STATUS_BAD_STATE);
            else if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            LSPString tmp;
            if (!tmp.set_utf8(path))
                return set_error(STATUS_NO_MEM);
            return open(&tmp);
        }

        status_t InFileStream::open(const LSPString *path)
        {
            if (pFD != NULL)
                return set_error(STATUS_BAD_STATE);
            else if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            NativeFile *f = new NativeFile();
            if (f == NULL)
                return set_error(STATUS_NO_MEM);

            status_t res = f->open(path, File::FM_READ);
            if (res != STATUS_OK)
            {
                f->close();
                delete f;
                return set_error(res);
            }

            res = wrap(f, WRAP_CLOSE | WRAP_DELETE);
            if (res != STATUS_OK)
            {
                f->close();
                delete f;
            }

            return set_error(res);
        }

        status_t InFileStream::open(const Path *path)
        {
            return open(path->as_string());
        }

        ssize_t InFileStream::read(void *dst, size_t count)
        {
            if (pFD == NULL)
                return set_error(STATUS_CLOSED);
            ssize_t res = pFD->read(dst, count);
            set_error((res >= 0) ? STATUS_OK : status_t(-res));
            return res;
        }

        wssize_t InFileStream::seek(wsize_t position)
        {
            if (pFD == NULL)
                return set_error(STATUS_CLOSED);
            status_t res = pFD->seek(position, File::FSK_SET);
            if (res != STATUS_OK)
                return -set_error(res);
            wssize_t pos = pFD->position();
            set_error((pos >= 0) ? STATUS_OK : status_t(-pos));
            return pos;
        }
    } /* namespace io */
} /* namespace lsp */
