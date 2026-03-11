/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 18 июн. 2018 г.
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

#include <errno.h>
#include <core/io/charset.h>
#include <core/io/StdioFile.h>
#include <core/io/NativeFile.h>
#include <core/io/OutFileStream.h>
#include <core/io/OutSequence.h>

namespace lsp
{
    namespace io
    {
        OutSequence::OutSequence()
        {
            pOS         = NULL;
            nWrapFlags  = 0;
        }

        OutSequence::~OutSequence()
        {
            if (pOS != NULL)
            {
                flush_buffer_internal(true);

                if (nWrapFlags & WRAP_CLOSE)
                    pOS->close();
                if (nWrapFlags & WRAP_DELETE)
                    delete pOS;
                pOS         = NULL;
            }
            nWrapFlags  = 0;

            sEncoder.close();
        }

        status_t OutSequence::close()
        {
            status_t res = STATUS_OK, tres;

            if (pOS != NULL)
            {
                res = flush();

                if (nWrapFlags & WRAP_CLOSE)
                {
                    tres = pOS->close();
                    if (res == STATUS_OK)
                        res = tres;
                }
                if (nWrapFlags & WRAP_DELETE)
                    delete pOS;
                pOS         = NULL;
            }
            nWrapFlags  = 0;

            sEncoder.close();

            return set_error(res);
        }
    
        status_t OutSequence::wrap(IOutStream *os, size_t flags, const char *charset)
        {
            if (pOS != NULL)
                return set_error(STATUS_BAD_STATE);
            else if (os == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            status_t res = sEncoder.init(charset);
            if (res != STATUS_OK)
            {
                sEncoder.close();
                return set_error(res);
            }

            pOS         = os;
            nWrapFlags  = flags;

            return set_error(STATUS_OK);
        }

        status_t OutSequence::open(const char *path, size_t mode, const char *charset)
        {
            if (pOS != NULL)
                return set_error(STATUS_BAD_STATE);
            else if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            LSPString tmp;
            if (!tmp.set_utf8(path))
                return set_error(STATUS_NO_MEM);
            return open(&tmp, mode, charset);
        }

        status_t OutSequence::open(const LSPString *path, size_t mode, const char *charset)
        {
            if (pOS != NULL)
                return set_error(STATUS_BAD_STATE);
            else if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            OutFileStream *f = new OutFileStream();
            if (f == NULL)
                return set_error(STATUS_NO_MEM);
            status_t res = f->open(path, mode);
            if (res != STATUS_OK)
            {
                f->close();
                delete f;
                return set_error(res);
            }

            res = wrap(f, WRAP_CLOSE | WRAP_DELETE, charset);
            if (res != STATUS_OK)
            {
                f->close();
                delete f;
                return set_error(res);
            }

            return set_error(STATUS_OK);
        }

        status_t OutSequence::open(const Path *path, size_t mode, const char *charset)
        {
            return open(path->as_string(), mode, charset);
        }

        status_t OutSequence::flush_buffer_internal(bool force)
        {
            ssize_t fetch;
            do
            {
                fetch = sEncoder.fetch(pOS);
            } while (fetch > 0);

            if ((fetch < 0) && (fetch != -STATUS_EOF))
                return set_error(-fetch);

            return set_error((force) ? pOS->flush() : STATUS_OK);
        }

        status_t OutSequence::write(lsp_wchar_t c)
        {
            if (pOS == NULL)
                return set_error(STATUS_CLOSED);

            ssize_t filled = sEncoder.fill(c);
            if (filled > 0)
                return set_error(STATUS_OK);

            status_t res = flush_buffer_internal(false);
            if (res != STATUS_OK)
                return set_error(res);

            filled = sEncoder.fill(c);
            return set_error((filled > 0) ? STATUS_OK : STATUS_UNKNOWN_ERR);
        }

        status_t OutSequence::write(const lsp_wchar_t *c, size_t count)
        {
            if (pOS == NULL)
                return set_error(STATUS_CLOSED);

            size_t written = 0;

            while (written < count)
            {
                ssize_t filled = sEncoder.fill(c, count - written);
                if (filled > 0)
                {
                    c          += filled;
                    written    += filled;
                    continue;
                }

                ssize_t fetched = sEncoder.fetch(pOS);
                if (fetched > 0)
                    continue;

                if (written > 0)
                    break;

                if (filled < 0)
                    return -set_error(-filled);
                else if (fetched < 0)
                    return -set_error(-fetched);

                break;
            }

            return set_error(STATUS_OK);
        }

        status_t OutSequence::flush()
        {
            if (pOS == NULL)
                return set_error(STATUS_CLOSED);

            return flush_buffer_internal(true);
        }
    } /* namespace io */
} /* namespace lsp */
