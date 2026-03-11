/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 16 июн. 2018 г.
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
#include <core/io/InFileStream.h>
#include <core/io/InSequence.h>

namespace lsp
{
    namespace io
    {
        InSequence::InSequence()
        {
            pIS         = NULL;
            nWrapFlags  = 0;
        }

        InSequence::~InSequence()
        {
            if (pIS != NULL)
            {
                if (nWrapFlags & WRAP_CLOSE)
                    pIS->close();
                if (nWrapFlags & WRAP_DELETE)
                    delete pIS;
                pIS         = NULL;
            }
            nWrapFlags  = 0;

            sDecoder.close();
        }

        status_t InSequence::close()
        {
            status_t res = STATUS_OK;

            if (pIS != NULL)
            {
                if (nWrapFlags & WRAP_CLOSE)
                    res = pIS->close();
                if (nWrapFlags & WRAP_DELETE)
                    delete pIS;
                pIS         = NULL;
            }
            nWrapFlags  = 0;

            sDecoder.close();

            return set_error(res);
        }
    
        status_t InSequence::wrap(IInStream *is, size_t flags, const char *charset)
        {
            if (pIS != NULL)
                return set_error(STATUS_BAD_STATE);
            else if (is == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            status_t res = sDecoder.init(charset);
            if (res != STATUS_OK)
            {
                sDecoder.close();
                return set_error(res);
            }

            pIS         = is;
            nWrapFlags  = flags;

            return set_error(STATUS_OK);
        }

        status_t InSequence::open(const char *path, const char *charset)
        {
            if (pIS != NULL)
                return set_error(STATUS_BAD_STATE);
            else if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            LSPString tmp;
            if (!tmp.set_utf8(path))
                return set_error(STATUS_NO_MEM);
            return open(&tmp, charset);
        }

        status_t InSequence::open(const LSPString *path, const char *charset)
        {
            if (pIS != NULL)
                return set_error(STATUS_BAD_STATE);
            else if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            InFileStream *is = new InFileStream();
            status_t res = is->open(path);
            if (res != STATUS_OK)
            {
                is->close();
                delete is;
                return set_error(res);
            }

            res = wrap(is, WRAP_CLOSE | WRAP_DELETE, charset);
            if (res != STATUS_OK)
            {
                is->close();
                delete is;
                return set_error(res);
            }

            return set_error(res);
        }

        status_t InSequence::open(const Path *path, const char *charset)
        {
            return open(path->as_string(), charset);
        }

        ssize_t InSequence::read(lsp_wchar_t *dst, size_t count)
        {
            if (pIS == NULL)
                return -set_error(STATUS_CLOSED);

            sLine.clear();

            size_t n_read = 0;
            while (n_read < count)
            {
                ssize_t fetched = sDecoder.fetch(dst, count - n_read);
                if (fetched > 0)
                {
                    n_read     += fetched;
                    dst        += fetched;
                    continue;
                }

                ssize_t filled  = sDecoder.fill(pIS);
                if (filled > 0)
                    continue;

                if (n_read > 0)
                    break;

                if (fetched < 0)
                    return -set_error(-fetched);
                else if (filled < 0)
                    return -set_error(-filled);

                set_error(STATUS_OK);
                break;
            }

            return n_read;
        }

        lsp_swchar_t InSequence::read_internal()
        {
            lsp_swchar_t ch = sDecoder.fetch();
            if (ch < 0)
            {
                if (ch != -STATUS_EOF)
                    return -set_error(-ch);

                ssize_t filled  = sDecoder.fill(pIS);
                if (filled < 0)
                    return -set_error(-filled);
                else if (filled == 0)
                    return -set_error(STATUS_EOF);

                ch  = sDecoder.fetch();
                if (ch < 0)
                    return -set_error(-ch);
            }
            return ch;
        }

        lsp_swchar_t InSequence::read()
        {
            if (pIS == NULL)
                return -set_error(STATUS_CLOSED);

            sLine.clear();
            return read_internal();
        }

        status_t InSequence::read_line(LSPString *s, bool force)
        {
            if (pIS == NULL)
                return set_error(STATUS_CLOSED);

            while (true)
            {
                lsp_swchar_t ch = read_internal();
                if (ch < 0)
                {
                    if (ch == -STATUS_EOF)
                        break;
                    return set_error(-ch);
                }

                if (ch == '\n')
                {
                    if (sLine.last() == '\r')
                        sLine.set_length(sLine.length() - 1);
                    s->take(&sLine);
                    return set_error(STATUS_OK);
                }

                if (!sLine.append(lsp_wchar_t(ch)))
                    return set_error(STATUS_NO_MEM);
            }

            if ((force) && (sLine.length() > 0))
            {
                s->take(&sLine);
                return set_error(STATUS_OK);
            }

            return set_error(STATUS_EOF);
        }
    } /* namespace io */
} /* namespace lsp */
