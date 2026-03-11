/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 30 мая 2019 г.
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

#include <core/KVTStorage.h>
#include <core/stdlib/string.h>

#include <stdlib.h>
#include <string.h>

namespace lsp
{
    KVTListener::KVTListener()
    {
    }

    KVTListener::~KVTListener()
    {
    }

    void KVTListener::attached(KVTStorage *storage)
    {
    }

    void KVTListener::detached(KVTStorage *storage)
    {
    }

    void KVTListener::created(KVTStorage *storage, const char *id, const kvt_param_t *param, size_t pending)
    {
    }

    void KVTListener::rejected(KVTStorage *storage, const char *id, const kvt_param_t *rej, const kvt_param_t *curr, size_t pending)
    {
    }

    void KVTListener::changed(KVTStorage *storage, const char *id, const kvt_param_t *oval, const kvt_param_t *nval, size_t pending)
    {
    }

    void KVTListener::removed(KVTStorage *storage, const char *id, const kvt_param_t *param, size_t pending)
    {
    }

    void KVTListener::access(KVTStorage *storage, const char *id, const kvt_param_t *param, size_t pending)
    {
    }

    void KVTListener::commit(KVTStorage *storage, const char *id, const kvt_param_t *param, size_t pending)
    {
    }

    void KVTListener::missed(KVTStorage *storage, const char *id)
    {
    }

    KVTStorage::KVTStorage(char separator)
    {
        cSeparator  = separator;
        sValid.next         = NULL;
        sValid.prev         = NULL;
        sTx.next            = NULL;
        sTx.prev            = NULL;
        sTx.node            = NULL;
        sRx.next            = NULL;
        sRx.prev            = NULL;
        sRx.node            = NULL;
        sGarbage.next       = NULL;
        sGarbage.prev       = NULL;
        sGarbage.node       = NULL;
        pTrash              = NULL;
        pIterators          = NULL;
        nNodes              = 0;
        nValues             = 0;
        nTxPending          = 0;
        nRxPending          = 0;

        init_node(&sRoot, NULL, 0);
        ++sRoot.refs;
    }
    
    KVTStorage::~KVTStorage()
    {
        destroy();
    }

    void KVTStorage::destroy()
    {
        unbind_all();

        while (pTrash != NULL)
        {
            kvt_gcparam_t *next = pTrash->next;
            destroy_parameter(pTrash);
            pTrash      = next;
        }

        while (pIterators != NULL)
        {
            KVTIterator *next   = pIterators->pGcNext;
            delete pIterators;
            pIterators          = next;
        }

        kvt_link_t *link = sValid.next;
        while (link != NULL)
        {
            kvt_link_t *next = link->next;
            destroy_node(link->node);
            link        = next;
        }
        link = sGarbage.next;
        while (link != NULL)
        {
            kvt_link_t *next = link->next;
            destroy_node(link->node);
            link        = next;
        }

        sRoot.id            = NULL;
        sRoot.idlen         = 0;
        sRoot.parent        = NULL;
        sRoot.refs          = 0;
        sRoot.param         = NULL;
        sRoot.gc.next       = NULL;
        sRoot.gc.prev       = NULL;
        sRoot.gc.node       = NULL;
        if (sRoot.children != NULL)
        {
            ::free(sRoot.children);
            sRoot.children      = NULL;
        }
        sRoot.nchildren     = 0;
        sRoot.capacity      = 0;

        sValid.next         = NULL;
        sValid.prev         = NULL;
        sTx.next            = NULL;
        sTx.prev            = NULL;
        sRx.next            = NULL;
        sRx.prev            = NULL;
        sGarbage.next       = NULL;
        sGarbage.prev       = NULL;
        pTrash              = NULL;
        pIterators          = NULL;

        nNodes              = 0;
        nValues             = 0;
        nTxPending          = 0;
        nRxPending          = 0;
    }

    status_t KVTStorage::clear()
    {
        return do_remove_branch("/", &sRoot);
    }

    void KVTStorage::init_node(kvt_node_t *node, const char *name, size_t len)
    {
        node->id            = (name != NULL) ? reinterpret_cast<char *>(&node[1]) : NULL;
        node->idlen         = len;
        node->parent        = NULL;
        node->refs          = 0;
        node->param         = NULL;
        node->pending       = 0;
        node->gc.next       = NULL;
        node->gc.prev       = NULL;
        node->gc.node       = node;
        node->tx.next       = NULL;
        node->tx.prev       = NULL;
        node->tx.node       = node;
        node->rx.next       = NULL;
        node->rx.prev       = NULL;
        node->rx.node       = node;
        node->children      = NULL;
        node->nchildren     = 0;
        node->capacity      = 0;

        if (node->id != NULL)
        {
            ::memcpy(node->id, name, len);
            node->id[len]       = '\0';
        }
    }

    size_t KVTStorage::listeners() const
    {
        return vListeners.size();
    }

    KVTStorage::kvt_node_t *KVTStorage::allocate_node(const char *name, size_t len)
    {
        size_t to_alloc     = sizeof(kvt_node_t) + len + 1;
        kvt_node_t *node    = reinterpret_cast<kvt_node_t *>(::malloc(to_alloc));
        if (node != NULL)
        {
            init_node(node, name, len);
            link_list(&sGarbage, &node->gc);
        }
        return node;
    }

    void KVTStorage::link_list(kvt_link_t *root, kvt_link_t *item)
    {
        item->next          = root->next;
        item->prev          = root;
        if (root->next != NULL)
            root->next->prev    = item;
        root->next          = item;
    }

    void KVTStorage::unlink_list(kvt_link_t *item)
    {
        if (item->prev != NULL)
            item->prev->next    = item->next;
        if (item->next != NULL)
            item->next->prev    = item->prev;
        item->next      = NULL;
        item->prev      = NULL;
    }

    size_t KVTStorage::set_pending_state(kvt_node_t *node, size_t flags)
    {
        if ((node->pending ^ flags) & KVT_TX)
        {
            if (flags & KVT_TX)
            {
                link_list(&sTx, &node->tx);
                node->pending  |= KVT_TX;
                ++nTxPending;
            }
            else
            {
                unlink_list(&node->tx);
                node->pending  &= ~KVT_TX;
                --nTxPending;
            }
        }

        if ((node->pending ^ flags) & KVT_RX)
        {
            if (flags & KVT_RX)
            {
                link_list(&sRx, &node->rx);
                node->pending  |= KVT_RX;
                ++nRxPending;
            }
            else
            {
                unlink_list(&node->rx);
                node->pending  &= ~KVT_RX;
                --nRxPending;
            }
        }

        return node->pending;
    }

    KVTStorage::kvt_node_t *KVTStorage::reference_up(kvt_node_t *node)
    {
        kvt_node_t *x = node;

        do
        {
            if ((x->refs++) > 0)
                break;

            unlink_list(&x->gc);
            link_list(&sValid, &x->gc);
            ++nNodes;

            x = x->parent;
        } while (x != NULL);

        return node;
    }

    KVTStorage::kvt_node_t *KVTStorage::reference_down(kvt_node_t *node)
    {
        kvt_node_t *x = node;

        do
        {
            if ((--x->refs) > 0)
                break;

            unlink_list(&x->gc);
            link_list(&sGarbage, &x->gc);
            --nNodes;

            x = x->parent;
        } while (x != NULL);

        return node;
    }

    void KVTStorage::notify_created(const char *id, const kvt_param_t *param, size_t pending)
    {
        for (size_t i=0, n=vListeners.size(); i<n; ++i)
        {
            KVTListener *listener = vListeners.at(i);
            if (listener != NULL)
                listener->created(this, id, param, pending);
        }
    }

    void KVTStorage::notify_changed(const char *id, const kvt_param_t *oval, const kvt_param_t *nval, size_t pending)
    {
        for (size_t i=0, n=vListeners.size(); i<n; ++i)
        {
            KVTListener *listener = vListeners.at(i);
            if (listener != NULL)
                listener->changed(this, id, oval, nval, pending);
        }
    }

    void KVTStorage::notify_removed(const char *id, const kvt_param_t *param, size_t pending)
    {
        for (size_t i=0, n=vListeners.size(); i<n; ++i)
        {
            KVTListener *listener = vListeners.at(i);
            if (listener != NULL)
                listener->removed(this, id, param, pending);
        }
    }

    void KVTStorage::notify_missed(const char *id)
    {
        for (size_t i=0, n=vListeners.size(); i<n; ++i)
        {
            KVTListener *listener = vListeners.at(i);
            if (listener != NULL)
                listener->missed(this, id);
        }
    }

    void KVTStorage::destroy_parameter(kvt_gcparam_t *param)
    {
        if (param->type == KVT_STRING)
        {
            if (param->str != NULL)
                ::free(const_cast<char *>(param->str));
            param->u64      = 0;
        }
        else if (param->type == KVT_BLOB)
        {
            if (param->blob.ctype != NULL)
            {
                ::free(const_cast<char *>(param->blob.ctype));
                param->blob.ctype   = NULL;
            }
            if (param->blob.data != NULL)
            {
                ::free(const_cast<void *>(param->blob.data));
                param->blob.data    = NULL;
            }
            param->blob.size    = 0;
        }
        else
            param->u64      = 0;

        param->type         = KVT_ANY;
        ::free(param);
    }

    void KVTStorage::destroy_node(kvt_node_t *node)
    {
        if (node->children != NULL)
        {
            ::free(node->children);
            node->children = NULL;
        }
        if (node->param != NULL)
        {
            destroy_parameter(node->param);
            node->param = NULL;
        }
        ::free(node);
    }

    status_t KVTStorage::put(const char *name, const kvt_param_t *value, size_t flags)
    {
        if ((name == NULL) || (value == NULL))
            return STATUS_BAD_ARGUMENTS;
        if (!validate_type(value->type))
            return STATUS_BAD_TYPE;

        return STATUS_OK;
    }

    status_t KVTStorage::get(const char *name, const kvt_param_t **value, kvt_param_type_t type)
    {
        if (name == NULL)
            return STATUS_BAD_ARGUMENTS;

        if (value != NULL)
            *value = NULL;

        return STATUS_NOT_FOUND;
    }

    bool KVTStorage::exists(const char *name, kvt_param_type_t type)
    {
        if (name == NULL)
            return false;
        return false;
    }

    status_t KVTStorage::remove(const char *name, const kvt_param_t **value, kvt_param_type_t type)
    {
        if (name == NULL)
            return STATUS_BAD_ARGUMENTS;

        if (value != NULL)
            *value = NULL;

        return STATUS_NOT_FOUND;
    }

    status_t KVTStorage::touch(const char *name, size_t flags)
    {
        if (name == NULL)
            return STATUS_BAD_ARGUMENTS;
        return STATUS_OK;
    }

    status_t KVTStorage::commit(const char *name, size_t flags)
    {
        if (name == NULL)
            return STATUS_BAD_ARGUMENTS;
        return STATUS_OK;
    }

    status_t KVTStorage::touch_all(size_t flags)
    {
        return STATUS_OK;
    }

    status_t KVTStorage::commit_all(size_t flags)
    {
        return STATUS_OK;
    }

    status_t KVTStorage::bind(KVTListener *listener)
    {
        if (listener == NULL)
            return STATUS_BAD_ARGUMENTS;
        return vListeners.add(listener) ? STATUS_OK : STATUS_NO_MEM;
    }

    status_t KVTStorage::unbind(KVTListener *listener)
    {
        if (listener == NULL)
            return STATUS_BAD_ARGUMENTS;
        return vListeners.remove(listener) ? STATUS_OK : STATUS_NOT_FOUND;
    }

    void KVTStorage::unbind_all()
    {
        vListeners.clear();
    }

    KVTIterator *KVTStorage::enum_all()
    {
        return NULL;
    }

    KVTIterator *KVTStorage::enum_branch(const char *path)
    {
        return NULL;
    }

} /* namespace lsp */
