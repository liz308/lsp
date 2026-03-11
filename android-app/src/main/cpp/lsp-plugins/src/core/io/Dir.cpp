/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 18 мар. 2019 г.
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

#include <core/io/Dir.h>
#include <sys/stat.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>

namespace lsp
{
    namespace io
    {
        Dir::Dir()
        {
            nErrorCode  = STATUS_OK;
            nPosition   = 0;
            hDir        = NULL;
        }
        
        Dir::~Dir()
        {
            close();
        }

        status_t Dir::open(const char *path)
        {
            if (hDir != NULL)
                return set_error(STATUS_BAD_STATE);
            else if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            LSPString spath;
            if (!spath.set_utf8(path))
                return set_error(STATUS_NO_MEM);
            return open(&spath);
        }

        status_t Dir::open(const LSPString *path)
        {
            if (hDir != NULL)
                return set_error(STATUS_BAD_STATE);
            else if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            if (!sPath.set(path))
                return set_error(STATUS_NO_MEM);

            DIR *dh = ::opendir(path->get_native());
            if (dh == NULL)
            {
                sPath.clear();
                int error = errno;
                switch (error)
                {
                    case EACCES: return set_error(STATUS_PERMISSION_DENIED);
                    case EMFILE:
                    case ENFILE: return set_error(STATUS_TOO_BIG);
                    case ENOENT: return set_error(STATUS_NOT_FOUND);
                    case ENOMEM: return set_error(STATUS_NO_MEM);
                    case ENOTDIR: return set_error(STATUS_BAD_TYPE);
                    default:
                        return set_error(STATUS_UNKNOWN_ERR);
                }
            }

            hDir        = dh;
            nPosition   = 0;
            return set_error(STATUS_OK);
        }

        status_t Dir::open(const Path *path)
        {
            if (hDir != NULL)
                return set_error(STATUS_BAD_STATE);
            else if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);
            return open(path->as_string());
        }

        status_t Dir::rewind()
        {
            if (hDir == NULL)
                return set_error(STATUS_BAD_STATE);

            ::rewinddir(hDir);
            nPosition = 0;
            return set_error(STATUS_OK);
        }

        status_t Dir::read(LSPString *path, bool full)
        {
            if (hDir == NULL)
                return set_error(STATUS_BAD_STATE);
            else if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            LSPString out;
            errno = 0;
            struct dirent *dent = ::readdir(hDir);
            if (dent == NULL)
            {
                if (errno == 0)
                    return set_error(STATUS_EOF);
                return set_error(STATUS_UNKNOWN_ERR);
            }

            if (!out.set_native(dent->d_name))
                return set_error(STATUS_NO_MEM);

            if (full)
            {
                Path tmp;
                status_t res = tmp.set(&sPath);
                if (res == STATUS_OK)
                    res = tmp.append_child(&out);
                if (res == STATUS_OK)
                    res = (out.set(tmp.as_string())) ? STATUS_OK : STATUS_NO_MEM;
                if (res != STATUS_OK)
                    set_error(res);
            }

            path->swap(&out);
            return set_error(STATUS_OK);
        }

        status_t Dir::read(Path *path, bool full)
        {
            if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            LSPString xpath;
            status_t res = read(&xpath, false);
            if (res == STATUS_OK)
            {
                if (full)
                {
                    Path tmp;
                    res = tmp.set(&sPath);
                    if (res == STATUS_OK)
                        res = tmp.append_child(&xpath);
                    if (res == STATUS_OK)
                        path->take(&tmp);
                }
                else
                    res = path->set(&xpath);
            }
            return set_error(res);
        }

        status_t Dir::close()
        {
            if (hDir == NULL)
                return set_error(STATUS_BAD_STATE);

            if (::closedir(hDir) != 0)
            {
                int error = errno;
                if (error == EBADF)
                    return set_error(STATUS_BAD_STATE);
                else
                    return set_error(STATUS_IO_ERROR);
            }
            hDir    = NULL;
            nPosition = 0;
            return set_error(STATUS_OK);
        }

        status_t Dir::create(const char *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;

            LSPString spath;
            if (!spath.set_utf8(path))
                return STATUS_NO_MEM;
            return create(&spath);
        }

        status_t Dir::create(const Path *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;
            return create(path->as_string());
        }

        status_t Dir::create(const LSPString *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;

            if (::mkdir(path->get_native(), S_IRWXU | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH) == 0)
                return STATUS_OK;

            int code = errno;
            switch (code)
            {
                case EACCES:
                case EPERM:
                    return STATUS_PERMISSION_DENIED;
                case EDQUOT:
                case ENOSPC:
                    return STATUS_OVERFLOW;
                case ENOTDIR:
                    return STATUS_BAD_HIERARCHY;
                case EFAULT:
                case EINVAL:
                case ENAMETOOLONG:
                    return STATUS_BAD_ARGUMENTS;
                case EEXIST:
                {
                    fattr_t attr;
                    File::stat(path, &attr);
                    if (attr.type == fattr_t::FT_DIRECTORY)
                        return STATUS_OK;
                    return STATUS_ALREADY_EXISTS;
                }
                case ENOENT:
                    return STATUS_NOT_FOUND;
                default:
                    return STATUS_IO_ERROR;
            }
        }

        status_t Dir::remove(const char *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;

            LSPString spath;
            if (!spath.set_utf8(path))
                return STATUS_NO_MEM;
            return remove(&spath);
        }

        status_t Dir::remove(const Path *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;
            return remove(path->as_string());
        }

        status_t Dir::remove(const LSPString *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;

            if (::rmdir(path->get_native()) == 0)
                return STATUS_OK;

            int code = errno;
            switch (code)
            {
                case EACCES:
                case EPERM:
                    return STATUS_PERMISSION_DENIED;
                case EDQUOT:
                case ENOSPC:
                    return STATUS_OVERFLOW;
                case EFAULT:
                case EINVAL:
                case ENAMETOOLONG:
                    return STATUS_BAD_ARGUMENTS;
                case ENOTDIR:
                    return STATUS_NOT_DIRECTORY;
                case ENOENT:
                    return STATUS_NOT_FOUND;
                case ENOTEMPTY:
                    return STATUS_NOT_EMPTY;
                default:
                    return STATUS_IO_ERROR;
            }
        }

        status_t Dir::get_current(LSPString *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;

            char spath[PATH_MAX];
            char *p = ::getcwd(spath, PATH_MAX);
            if (p == NULL)
            {
                int code = errno;
                switch (code)
                {
                    case EACCES:
                    case EPERM:
                        return STATUS_PERMISSION_DENIED;
                    case ENOENT:
                        return STATUS_REMOVED;
                    case ENAMETOOLONG:
                        return STATUS_OVERFLOW;
                    case ENOMEM:
                        return STATUS_NO_MEM;
                    default:
                        return STATUS_IO_ERROR;
                }
            }

            return (path->set_native(p)) ? STATUS_OK : STATUS_NO_MEM;
        }

        status_t Dir::get_current(Path *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;
            LSPString tmp;
            status_t res = get_current(&tmp);
            if (res == STATUS_OK)
                path->take(&tmp);
            return res;
        }
    } /* namespace io */
} /* namespace lsp */
