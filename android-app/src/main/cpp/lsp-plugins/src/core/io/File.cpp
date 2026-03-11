/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 6 мар. 2019 г.
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

#include <core/io/File.h>
#include <core/debug.h>
#include <sys/stat.h>
#include <errno.h>
#include <unistd.h>

namespace lsp
{
    namespace io
    {
        File::File()
        {
            nErrorCode  = STATUS_OK;
        }
        
        File::~File()
        {
            close();
        }

        ssize_t File::read(void *dst, size_t count)
        {
            return -set_error(STATUS_NOT_SUPPORTED);
        }

        ssize_t File::write(const void *src, size_t count)
        {
            return -set_error(STATUS_NOT_SUPPORTED);
        }

        status_t File::seek(wssize_t pos, size_t type)
        {
            return set_error(STATUS_NOT_SUPPORTED);
        }

        wssize_t File::position()
        {
            return -set_error(STATUS_NOT_SUPPORTED);
        }

        wssize_t File::size()
        {
            return -set_error(STATUS_NOT_SUPPORTED);
        }

        status_t File::stat(fattr_t *attr)
        {
            return -set_error(STATUS_NOT_SUPPORTED);
        }

        status_t File::close()
        {
            return set_error(STATUS_OK);
        }
    
        status_t File::stat(const char *path, fattr_t *attr)
        {
            if ((path == NULL) || (attr == NULL))
                return STATUS_BAD_ARGUMENTS;

            LSPString spath;
            if (!spath.set_utf8(path))
                return STATUS_NO_MEM;
            return stat(&spath, attr);
        }

        status_t File::stat(const LSPString *path, fattr_t *attr)
        {
            if ((path == NULL) || (attr == NULL))
                return STATUS_BAD_ARGUMENTS;

            struct stat sb;
            if (::lstat(path->get_native(), &sb) != 0)
            {
                int code = errno;
                switch (code)
                {
                    case EACCES: return STATUS_PERMISSION_DENIED;
                    case EBADF: return STATUS_INVALID_VALUE;
                    case ENAMETOOLONG: return STATUS_OVERFLOW;
                    case EOVERFLOW: return STATUS_OVERFLOW;
                    case ENOENT: return STATUS_NOT_FOUND;
                    case ENOMEM: return STATUS_NO_MEM;
                    default: break;
                }
                return STATUS_IO_ERROR;
            }

            switch (sb.st_mode & S_IFMT) {
                case S_IFBLK:  attr->type = fattr_t::FT_BLOCK;      break;
                case S_IFCHR:  attr->type = fattr_t::FT_CHARACTER;  break;
                case S_IFDIR:  attr->type = fattr_t::FT_DIRECTORY;  break;
                case S_IFIFO:  attr->type = fattr_t::FT_FIFO;       break;
                case S_IFLNK:  attr->type = fattr_t::FT_SYMLINK;    break;
                case S_IFREG:  attr->type = fattr_t::FT_REGULAR;    break;
                case S_IFSOCK: attr->type = fattr_t::FT_SOCKET;     break;
                default:       attr->type = fattr_t::FT_UNKNOWN;    break;
            }

            attr->blk_size  = sb.st_blksize;
            attr->size      = sb.st_size;
            attr->inode     = sb.st_ino;
            attr->ctime     = (sb.st_ctim.tv_sec * 1000L) + (sb.st_ctim.tv_nsec / 1000000);
            attr->mtime     = (sb.st_mtim.tv_sec * 1000L) + (sb.st_mtim.tv_nsec / 1000000);
            attr->atime     = (sb.st_atim.tv_sec * 1000L) + (sb.st_atim.tv_nsec / 1000000);

            return STATUS_OK;
        }

        status_t File::remove(const char *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;

            LSPString spath;
            if (!spath.set_utf8(path))
                return STATUS_NO_MEM;
            return remove(&spath);
        }

        status_t File::remove(const LSPString *path)
        {
            if (path == NULL)
                return STATUS_BAD_ARGUMENTS;

            if (::unlink(path->get_native()) == 0)
                return STATUS_OK;

            int code = errno;
            switch (code)
            {
                case EACCES:
                    return STATUS_PERMISSION_DENIED;
                case EPERM:
                {
                    fattr_t attr;
                    status_t res = stat(path, &attr);
                    if ((res == STATUS_OK) && (attr.type == fattr_t::FT_DIRECTORY))
                        return STATUS_IS_DIRECTORY;
                    return STATUS_PERMISSION_DENIED;
                }
                case EISDIR:
                    return STATUS_IS_DIRECTORY;
                case ENOENT:
                    return STATUS_NOT_FOUND;
                case ENOTEMPTY:
                    return STATUS_NOT_EMPTY;
                default:
                    return STATUS_IO_ERROR;
            }
        }

    } /* namespace io */
} /* namespace lsp */
