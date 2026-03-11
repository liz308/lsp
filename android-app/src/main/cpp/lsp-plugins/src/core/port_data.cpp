/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 24 мая 2019 г.
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

#include <core/port_data.h>
#include <core/status.h>

#include <stdlib.h>
#include <string.h>

namespace lsp
{
    path_t::~path_t()
    {
    }

    void path_t::init()
    {
    }

    const char *path_t::get_path()
    {
        return "";
    }

    size_t path_t::get_flags()
    {
        return 0;
    }

    void path_t::accept()
    {
    }

    void path_t::commit()
    {
    }

    bool path_t::pending()
    {
        return false;
    }

    bool path_t::accepted()
    {
        return false;
    }

    mesh_t *mesh_t::create(size_t buffers, size_t length)
    {
        size_t buf_size     = length * sizeof(float);
        size_t mesh_size    = sizeof(mesh_t) + sizeof(float *) * buffers;

        buf_size            = (buf_size + 0x3f) & (~size_t(0x3f));
        mesh_size           = (mesh_size + 0x3f) & (~size_t(0x3f));

        uint8_t *ptr        = reinterpret_cast<uint8_t *>(malloc(mesh_size + buf_size * buffers));
        if (ptr == NULL)
            return NULL;

        mesh_t *mesh        = reinterpret_cast<mesh_t *>(ptr);
        mesh->nState        = M_EMPTY;
        mesh->nBuffers      = 0;
        mesh->nItems        = 0;
        ptr                += mesh_size;
        for (size_t i=0; i<buffers; ++i)
        {
            mesh->pvData[i]    = reinterpret_cast<float *>(ptr);
            ptr                += buf_size;
        }

        return mesh;
    }

    void mesh_t::destroy(mesh_t *mesh)
    {
        if (mesh == NULL)
            return;
        free(mesh);
    }

    position_t::position_t()
    {
        sampleRate     = 44100;
        speed          = 1.0;
        frame          = 0;
        numerator      = 4.0;
        denominator    = 4.0;
        beatsPerMinute = 120.0;
        tick           = 0;
        ticksPerBeat   = 960;
    }

    void position_t::init(position_t *pos)
    {
        pos->sampleRate     = 44100;
        pos->speed          = 1.0;
        pos->frame          = 0;
        pos->numerator      = 4.0;
        pos->denominator    = 4.0;
        pos->beatsPerMinute = 120.0;
        pos->tick           = 0;
        pos->ticksPerBeat   = 960;
    }

    frame_buffer_t *frame_buffer_t::create(size_t rows, size_t cols)
    {
        size_t cap          = rows * 4;
        size_t hcap         = 1;
        while (hcap < cap)
            hcap                <<= 1;

        size_t h_size       = sizeof(frame_buffer_t);
        size_t b_size       = hcap * cols * sizeof(float);

        uint8_t *ptr = reinterpret_cast<uint8_t *>(malloc(h_size + b_size));
        if (ptr == NULL)
            return NULL;

        frame_buffer_t *fb  = reinterpret_cast<frame_buffer_t *>(ptr);
        ptr                += h_size;

        fb->nRows           = rows;
        fb->nCols           = cols;
        fb->nCapacity       = hcap;
        fb->nRowID          = rows;
        fb->vData           = reinterpret_cast<float *>(ptr);
        fb->pData           = NULL;

        memset(fb->vData, 0, rows * cols * sizeof(float));
        return fb;
    }

    void frame_buffer_t::destroy(frame_buffer_t *buf)
    {
        if (buf != NULL)
            free(buf);
    }

    void frame_buffer_t::clear()
    {
        if (vData != NULL)
            memset(vData, 0, nCapacity * nCols * sizeof(float));
        nRowID += nRows;
    }

    void frame_buffer_t::seek(uint32_t row_id)
    {
        nRowID = row_id;
    }

    void frame_buffer_t::read_row(float *dst, size_t row_id) const
    {
        uint32_t off    = row_id & (nCapacity - 1);
        memcpy(dst, &vData[off * nCols], nCols * sizeof(float));
    }

    float *frame_buffer_t::get_row(size_t row_id) const
    {
        uint32_t off    = row_id & (nCapacity - 1);
        return &vData[off * nCols];
    }

    float *frame_buffer_t::next_row() const
    {
        uint32_t off    = nRowID & (nCapacity - 1);
        return &vData[off * nCols];
    }

    void frame_buffer_t::write_row(const float *row)
    {
        uint32_t off    = nRowID & (nCapacity - 1);
        memcpy(&vData[off * nCols], row, nCols * sizeof(float));
        ++nRowID;
    }

    void frame_buffer_t::write_row(uint32_t row_id, const float *row)
    {
        uint32_t off    = row_id & (nCapacity - 1);
        memcpy(&vData[off * nCols], row, nCols * sizeof(float));
    }

    void frame_buffer_t::write_row()
    {
        ++nRowID;
    }

    bool frame_buffer_t::sync(const frame_buffer_t *fb)
    {
        if (fb == NULL)
            return false;

        uint32_t src_rid = fb->nRowID, dst_rid = nRowID;
        uint32_t delta = src_rid - dst_rid;
        if (delta == 0)
            return false;
        else if (delta > nRows)
            dst_rid = src_rid - nRows;

        while (dst_rid != src_rid)
        {
            const float *row = fb->get_row(dst_rid);
            size_t off      = (dst_rid) & (nCapacity - 1);
            memcpy(&vData[off * nCols], row, nCols * sizeof(float));
            dst_rid++;
        }

        nRowID      = dst_rid;
        return true;
    }
}
