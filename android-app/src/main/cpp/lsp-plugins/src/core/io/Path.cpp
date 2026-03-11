/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 8 февр. 2019 г.
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

#include <core/io/Path.h>
#include <core/io/File.h>
#include <core/io/Dir.h>
#include <string.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <errno.h>

namespace lsp
{
    namespace io
    {
        Path::Path()
        {
        }
        
        Path::~Path()
        {
        }

        inline void Path::fixup_path()
        {
            sPath.replace_all('\\', '/');
        }

        status_t Path::set(const char *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;
            if (!sPath.set_utf8(path))
                return STATUS_NO_MEM;
            fixup_path();
            return STATUS_OK;
        }

        status_t Path::set(const LSPString *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;
            if (!sPath.set(path))
                return STATUS_NO_MEM;
            fixup_path();
            return STATUS_OK;
        }

        status_t Path::set(const Path *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;
            if (!sPath.set(&path->sPath))
                return STATUS_NO_MEM;
            fixup_path();
            return STATUS_OK;
        }

        status_t Path::set(const char *path, const char *child)
        {
            Path tmp;
            status_t res = tmp.set(path);
            if (res == STATUS_OK)
                res = tmp.append_child(child);
            if (res == STATUS_OK)
                swap(&tmp);
            return res;
        }

        status_t Path::set(const char *path, const LSPString *child)
        {
            Path tmp;
            status_t res = tmp.set(path);
            if (res == STATUS_OK)
                res = tmp.append_child(child);
            if (res == STATUS_OK)
                swap(&tmp);
            return res;
        }

        status_t Path::set(const char *path, const Path *child)
        {
            Path tmp;
            status_t res = tmp.set(path);
            if (res == STATUS_OK)
                res = tmp.append_child(child);
            if (res == STATUS_OK)
                swap(&tmp);
            return res;
        }

        status_t Path::set(const LSPString *path, const char *child)
        {
            Path tmp;
            status_t res = tmp.set(path);
            if (res == STATUS_OK)
                res = tmp.append_child(child);
            if (res == STATUS_OK)
                swap(&tmp);
            return res;
        }

        status_t Path::set(const LSPString *path, const LSPString *child)
        {
            Path tmp;
            status_t res = tmp.set(path);
            if (res == STATUS_OK)
                res = tmp.append_child(child);
            if (res == STATUS_OK)
                swap(&tmp);
            return res;
        }

        status_t Path::set(const LSPString *path, const Path *child)
        {
            Path tmp;
            status_t res = tmp.set(path);
            if (res == STATUS_OK)
                res = tmp.append_child(child);
            if (res == STATUS_OK)
                swap(&tmp);
            return res;
        }

        status_t Path::set(const Path *path, const char *child)
        {
            Path tmp;
            status_t res = tmp.set(path);
            if (res == STATUS_OK)
                res = tmp.append_child(child);
            if (res == STATUS_OK)
                swap(&tmp);
            return res;
        }

        status_t Path::set(const Path *path, const LSPString *child)
        {
            Path tmp;
            status_t res = tmp.set(path);
            if (res == STATUS_OK)
                res = tmp.append_child(child);
            if (res == STATUS_OK)
                swap(&tmp);
            return res;
        }

        status_t Path::set(const Path *path, const Path *child)
        {
            Path tmp;
            status_t res = tmp.set(path);
            if (res == STATUS_OK)
                res = tmp.append_child(child);
            if (res == STATUS_OK)
                swap(&tmp);
            return res;
        }

        status_t Path::get(char *path, size_t maxlen) const
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;

            const char *utf8 = sPath.get_utf8();
            if (utf8 == NULL)
                return STATUS_NO_MEM;

            size_t len = ::strlen(utf8);
            if (len >= maxlen)
                return STATUS_TOO_BIG;

            ::memcpy(path, utf8, len+1);
            return STATUS_OK;
        }

        status_t Path::get(LSPString *path) const
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;

            return (path->set(&sPath)) ? STATUS_OK : STATUS_NO_MEM;
        }

        status_t Path::get(Path *path) const
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;

            return (path->sPath.set(&sPath)) ? STATUS_OK : STATUS_NO_MEM;
        }

        status_t Path::append_child(const char *path)
        {
            Path tmp;
            status_t res = tmp.set(path);
            if (res != STATUS_OK)
                return res;
            else if (tmp.is_empty())
                return STATUS_OK;
            else if (tmp.is_absolute())
                return STATUS_INVALID_VALUE;

            size_t len = sPath.length();
            bool success = ((len <= 0) || (sPath.ends_with(FILE_SEPARATOR_C))) ? true : sPath.append(FILE_SEPARATOR_C);
            if (success)
                success = sPath.append(&tmp.sPath);
            if (success)
                fixup_path();
            else
                sPath.set_length(len);

            return (success) ? STATUS_OK : STATUS_NO_MEM;
        }

        status_t Path::append_child(const LSPString *path)
        {
            Path tmp;
            status_t res = tmp.set(path);
            if (res != STATUS_OK)
                return res;
            else if (tmp.is_empty())
                return STATUS_OK;
            else if (tmp.is_absolute())
                return STATUS_INVALID_VALUE;

            size_t len = sPath.length();
            bool success = ((len <= 0) || (sPath.ends_with(FILE_SEPARATOR_C))) ? true : sPath.append(FILE_SEPARATOR_C);
            if (success)
                success = sPath.append(&tmp.sPath);
            if (success)
                fixup_path();
            else
                sPath.set_length(len);

            return (success) ? STATUS_OK : STATUS_NO_MEM;
        }

        status_t Path::append_child(const Path *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;
            else if (path->is_empty())
                return STATUS_OK;
            else if (path->is_absolute())
                return STATUS_INVALID_VALUE;

            size_t len = sPath.length();
            bool success = ((len <= 0) || (sPath.ends_with(FILE_SEPARATOR_C))) ? true : sPath.append(FILE_SEPARATOR_C);
            if (success)
                success = sPath.append(&path->sPath);
            if (success)
                fixup_path();
            else
                sPath.set_length(len);
            return (success) ? STATUS_OK : STATUS_NO_MEM;
        }

        status_t Path::remove_last()
        {
            if (is_root())
                return STATUS_OK;

            ssize_t idx     = sPath.rindex_of(FILE_SEPARATOR_C);
            if (is_relative())
            {
                if (idx < 0)
                    idx             = 0;
                sPath.set_length(idx);
            }
            else if (idx > 0)
            {
                ssize_t idx2    = sPath.rindex_of(idx - 1, FILE_SEPARATOR_C);
                if (idx2 < 0)
                    idx             = idx + 1;
                sPath.set_length(idx);
            }
            return STATUS_OK;
        }

        status_t Path::parent()
        {
            if (is_root())
                return STATUS_OK;
            ssize_t idx = sPath.rindex_of(FILE_SEPARATOR_C);
            if (idx < 0)
                idx = 0;
            sPath.set_length(idx);
            return STATUS_OK;
        }

        bool Path::is_absolute() const
        {
            if (sPath.length() <= 0)
                return false;
            return (sPath.first() == FILE_SEPARATOR_C);
        }

        bool Path::is_relative() const
        {
            if (sPath.length() <= 0)
                return true;
            return (sPath.first() != FILE_SEPARATOR_C);
        }

        bool Path::is_root() const
        {
            return (sPath.length() == 1) && (sPath.first() == FILE_SEPARATOR_C);
        }

        bool Path::is_empty() const
        {
            return sPath.length() <= 0;
        }

        status_t Path::canonicalize()
        {
            enum state_t
            {
                S_SEEK,
                S_SEPARATOR,
                S_DOT,
                S_DOTDOT
            };

            lsp_wchar_t c;
            size_t len              = sPath.length();
            lsp_wchar_t *s          = const_cast<lsp_wchar_t *>(sPath.characters());
            lsp_wchar_t *e          = &s[len];
            state_t state           = S_SEEK;

            if (is_absolute())
            {
                while (*(s++) != FILE_SEPARATOR_C)
                    /* loop */ ;
                state               = S_SEPARATOR;
            }

            lsp_wchar_t *p          = s;
            lsp_wchar_t *w          = s;

            while (p < e)
            {
                c  = *p++;

                switch (state)
                {
                    case S_SEEK:
                        if (c == FILE_SEPARATOR_C)
                        {
                            state       = S_SEPARATOR;
                            *w++        = c;
                        }
                        else if (c == '.')
                            state       = S_DOT;
                        else
                            *w++    = c;
                        break;
                    case S_SEPARATOR:
                        if (c == FILE_SEPARATOR_C)
                            break;
                        else if (c == '.')
                            state       = S_DOT;
                        else
                        {
                            *w++    = c;
                            state   = S_SEEK;
                        }
                        break;
                    case S_DOT:
                        if (c == FILE_SEPARATOR_C)
                            state       = S_SEPARATOR;
                        else if (c == '.')
                            state       = S_DOTDOT;
                        else
                        {
                            *w++        = '.';
                            *w++        = c;
                            state       = S_SEEK;
                        }
                        break;
                    case S_DOTDOT:
                        if (c == FILE_SEPARATOR_C)
                        {
                            state       = S_SEPARATOR;
                            do
                            {
                                if (w <= s)
                                    break;
                                --w;
                            }
                            while (w[-1] != FILE_SEPARATOR_C);
                        }
                        else
                        {
                            *w++        = '.';
                            *w++        = '.';
                            *w++        = c;
                            state       = S_SEEK;
                        }
                        break;
                }
            }

            while ((w > s) && (w[-1] == FILE_SEPARATOR_C))
                --w;

            sPath.set_length(w - const_cast<lsp_wchar_t *>(sPath.characters()));

            return STATUS_OK;
        }

        bool Path::equals(const Path *path) const
        {
            return (path != NULL) ? sPath.equals(&path->sPath) : false;
        }

        bool Path::equals(const LSPString *path) const
        {
            return (path != NULL) ? sPath.equals(path) : false;
        }

        bool Path::equals(const char *path) const
        {
            if (path == NULL)
                return false;

            LSPString tmp;
            return (tmp.set_utf8(path)) ? tmp.equals(&sPath) : false;
        }

        status_t Path::stat(fattr_t *attr) const
        {
            return File::stat(&sPath, attr);
        }

        wssize_t Path::size() const
        {
            fattr_t attr;
            status_t res = File::stat(&sPath, &attr);
            return (res != STATUS_OK) ? attr.size : -res;
        }

        bool Path::exists() const
        {
            fattr_t attr;
            status_t res = File::stat(&sPath, &attr);
            return res == STATUS_OK;
        }

        bool Path::is_dir() const
        {
            fattr_t attr;
            status_t res = File::stat(&sPath, &attr);
            return (res == STATUS_OK) && (attr.type == fattr_t::FT_DIRECTORY);
        }
    } /* namespace io */
} /* namespace lsp */
