/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 7 мар. 2019 г.
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
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>

#define BAD_FD      lsp_fhandle_t(-1)

namespace lsp
{
    namespace io
    {
        NativeFile::NativeFile()
        {
            hFD     = BAD_FD;
            nFlags  = 0;
        }
        
        NativeFile::~NativeFile()
        {
            if (hFD != BAD_FD)
            {
                if (nFlags & SF_CLOSE)
                    ::close(hFD);
                hFD     = BAD_FD;
            }
            nFlags  = 0;
        }

        status_t NativeFile::open(const char *path, size_t mode)
        {
            if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            LSPString tmp;
            if (!tmp.set_utf8(path))
                return set_error(STATUS_NO_MEM);
            return open(&tmp, mode);
        }

        status_t NativeFile::open(const LSPString *path, size_t mode)
        {
            if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);

            fattr_t stat;
            if (File::stat(path, &stat) == STATUS_OK)
            {
                if (stat.type == fattr_t::FT_DIRECTORY)
                    return (mode & FM_CREATE) ? STATUS_ALREADY_EXISTS : STATUS_NOT_FOUND;
            }

            int oflags;
            size_t fflags;
            if (mode & FM_READ)
            {
                oflags      = (mode & FM_WRITE) ? O_RDWR : O_RDONLY;
                fflags      = (mode & FM_WRITE) ? SF_READ | SF_WRITE : SF_READ;
            }
            else if (mode & FM_WRITE)
            {
                oflags      = O_WRONLY;
                fflags      = SF_WRITE;
            }
            else
                return set_error(STATUS_INVALID_VALUE);
            if (mode & FM_CREATE)
                oflags     |= O_CREAT;
            if (mode & FM_TRUNC)
                oflags     |= O_TRUNC;

            lsp_fhandle_t fd    = ::open(path->get_native(), oflags, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
            if (fd < 0)
            {
                int code = errno;
                status_t res = STATUS_IO_ERROR;

                switch (code)
                {
                    case EPERM: case EACCES: res = STATUS_PERMISSION_DENIED; break;
                    case EEXIST: res = STATUS_ALREADY_EXISTS; break;
                    case EINVAL: res = STATUS_INVALID_VALUE; break;
                    case EISDIR: res = STATUS_IS_DIRECTORY; break;
                    case ENAMETOOLONG: res = STATUS_OVERFLOW; break;
                    case ENOENT: res = STATUS_NOT_FOUND; break;
                    case ENOMEM: res = STATUS_NO_MEM; break;
                    case ENOTDIR: res = STATUS_NOT_DIRECTORY; break;
                    case EROFS: res = STATUS_READONLY; break;
                    default: break;
                }

                return set_error(res);
            }

            hFD         = fd;
            nFlags      = fflags | SF_CLOSE;

            return set_error(STATUS_OK);
        }

        status_t NativeFile::open(const Path *path, size_t mode)
        {
            if (path == NULL)
                return set_error(STATUS_BAD_ARGUMENTS);
            return open(path->as_string(), mode);
        }

        status_t NativeFile::wrap(lsp_fhandle_t fd, size_t mode, bool close)
        {
            if (hFD != BAD_FD)
                return set_error(STATUS_BAD_STATE);

            size_t flags = (close) ? SF_CLOSE : 0;
            if (mode & FM_READ)
                flags  |= SF_READ;
            if (mode & FM_WRITE)
                flags  |= SF_WRITE;

            hFD     = fd;
            nFlags  = flags;

            return set_error(STATUS_OK);
        }

        ssize_t NativeFile::read(void *dst, size_t count)
        {
            if (hFD == BAD_FD)
                return -set_error(STATUS_BAD_STATE);
            else if (!(nFlags & SF_READ))
                return -set_error(STATUS_PERMISSION_DENIED);

            uint8_t *ptr    = reinterpret_cast<uint8_t *>(dst);
            size_t bread    = 0;
            bool eof        = false;

            while (bread < count)
            {
                size_t to_read  = count - bread;
                ssize_t n_read   = ::read(hFD, ptr, to_read);

                if (n_read <= 0)
                {
                    eof = true;
                    break;
                }

                ptr    += n_read;
                bread  += n_read;
            }

            if ((bread > 0) || (count <= 0) || (!eof))
            {
                set_error(STATUS_OK);
                return bread;
            }
            return -set_error(STATUS_EOF);
        }

        ssize_t NativeFile::write(const void *buf, size_t count)
        {
            if (hFD == BAD_FD)
                return -set_error(STATUS_BAD_STATE);
            else if (!(nFlags & SF_WRITE))
                return -set_error(STATUS_PERMISSION_DENIED);

            const uint8_t *ptr  = reinterpret_cast<const uint8_t *>(buf);
            size_t bwritten     = 0;

            while (bwritten < count)
            {
                size_t to_write = count - bwritten;
                ssize_t n_written = ::write(hFD, ptr, to_write);

                if (n_written <= 0)
                    break;

                ptr        += n_written;
                bwritten   += n_written;
            }

            set_error((bwritten > 0) ? STATUS_OK : STATUS_IO_ERROR);
            return bwritten;
        }

        status_t NativeFile::close()
        {
            if (hFD == BAD_FD)
                return set_error(STATUS_BAD_STATE);

            if (::close(hFD) != 0)
                return set_error(STATUS_IO_ERROR);

            hFD     = BAD_FD;
            nFlags  = 0;
            return set_error(STATUS_OK);
        }

        status_t NativeFile::flush()
        {
            if (hFD == BAD_FD)
                return set_error(STATUS_BAD_STATE);

            if (::fsync(hFD) != 0)
                return set_error(STATUS_IO_ERROR);

            return set_error(STATUS_OK);
        }

        wssize_t NativeFile::position()
        {
            if (hFD == BAD_FD)
                return -set_error(STATUS_BAD_STATE);

            off_t pos = ::lseek(hFD, 0, SEEK_CUR);
            if (pos < 0)
                return -set_error(STATUS_IO_ERROR);

            set_error(STATUS_OK);
            return pos;
        }

        wssize_t NativeFile::size()
        {
            if (hFD == BAD_FD)
                return -set_error(STATUS_BAD_STATE);

            struct stat sb;
            if (::fstat(hFD, &sb) != 0)
                return -set_error(STATUS_IO_ERROR);

            set_error(STATUS_OK);
            return sb.st_size;
        }

        status_t NativeFile::seek(wsize_t position, size_t mode)
        {
            if (hFD == BAD_FD)
                return set_error(STATUS_BAD_STATE);

            int whence = SEEK_SET;
            if (mode == FSK_CUR)
                whence = SEEK_CUR;
            else if (mode == FSK_END)
                whence = SEEK_END;

            off_t pos = ::lseek(hFD, position, whence);
            if (pos < 0)
                return set_error(STATUS_IO_ERROR);

            return set_error(STATUS_OK);
        }
    } /* namespace io */
} /* namespace lsp */
