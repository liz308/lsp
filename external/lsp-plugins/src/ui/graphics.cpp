/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 27 нояб. 2015 г.
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

package lsp

import kotlin.math.*

data class Line2DEquation(val a: Float, val b: Float, val c: Float)
data class Point2D(val x: Float, val y: Float)

fun line2dEquation(
    x1: Float, y1: Float,
    x2: Float, y2: Float
): Line2DEquation? {
    val dx = x1 - x2
    val dy = y1 - y2

    if (dy == 0.0f && dx == 0.0f)
        return null

    val a = dy
    val b = -dx
    val c = y1 * dx - x1 * dy

    return Line2DEquation(a, b, c)
}

fun line2dEquation(
    dx: Float, dy: Float
): Line2DEquation? {
    if (dx == 0.0f && dy == 0.0f)
        return null

    val a = dy
    val b = -dx
    val c = 0.0f

    return Line2DEquation(a, b, c)
}

fun line2dIntersection(
    a1: Float, b1: Float, c1: Float,
    a2: Float, b2: Float, c2: Float
): Point2D? {
    var d = a1 * b2 - b1 * a2
    if (d == 0.0f)
        return null

    d = 1.0f / d
    val x = (b1 * c2 - b2 * c1) * d
    val y = (a2 * c1 - a1 * c2) * d

    return Point2D(x, y)
}

fun distance2d(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return sqrt(dx * dx + dy * dy)
}

fun scalarProduct2d(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    return x1 * x2 + y1 * y2
}

fun vectorProduct2d(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    return x1 * y2 - x2 * y1
}

fun getAngle2d(
    x0: Float, y0: Float,
    x: Float, y: Float
): Float {
    val dx = x - x0
    val dy = y - y0
    val l = sqrt(dx * dx + dy * dy)
    if (l == 0.0f)
        return 0.0f

    val a = if (dx < dy) { // calc with acos
        val angle = acos(dx / l) // 0..PI
        if (dy >= 0.0f) angle else PI.toFloat() * 2 - angle
    } else { // calc with asin
        val angle = asin(dy / l) // -PI/2 .. PI/2
        if (dx >= 0.0f) angle else PI.toFloat() - angle
    }

    return if (a < 0.0f) PI.toFloat() * 2 + a else a
}

fun clipLine2d(
    dx: Float, dy: Float,
    lc: Float, rc: Float, tc: Float, bc: Float
): Pair<Point2D, Point2D>? {
    return clipLine2d(dy, dx, 0.0f, lc, rc, tc, bc)
}

fun clipLine2d(
    x1: Float, x2: Float, y1: Float, y2: Float,
    lc: Float, rc: Float, tc: Float, bc: Float
): Pair<Point2D, Point2D>? {
    val equation = line2dEquation(x1, y1, x2, y2) ?: return null
    return clipLine2d(equation.a, equation.b, equation.c, lc, rc, tc, bc)
}

fun clipLine2d(
    a: Float, b: Float, c: Float,
    lc: Float, rc: Float, tc: Float, bc: Float
): Pair<Point2D, Point2D>? {
    val rx = FloatArray(4)
    val ry = FloatArray(4)
    var n = 0
    
        // Left corner
    line2dIntersection(a, b, c, -1.0f, 0.0f, lc)?.let { point ->
        if (clip2d(point.x, point.y, lc, rc, tc, bc)) {
            rx[n] = point.x
            ry[n] = point.y
            n++
        }
    }
    
        // Right corner
    line2dIntersection(a, b, c, -1.0f, 0.0f, rc)?.let { point ->
        if (clip2d(point.x, point.y, lc, rc, tc, bc)) {
            rx[n] = point.x
            ry[n] = point.y
            n++
        }
    }
    
        // Top corner
    line2dIntersection(a, b, c, 0.0f, -1.0f, tc)?.let { point ->
        if (clip2d(point.x, point.y, lc, rc, tc, bc)) {
            rx[n] = point.x
            ry[n] = point.y
            n++
        }
    }
    
        // Bottom corner
    line2dIntersection(a, b, c, 0.0f, -1.0f, bc)?.let { point ->
        if (clip2d(point.x, point.y, lc, rc, tc, bc)) {
            rx[n] = point.x
            ry[n] = point.y
            n++
        }
    }

        if (n <= 0)
        return null

    val cx1 = rx[0]
    val cy1 = ry[0]
    val (cx2, cy2) = if (n < 2) {
        Pair(rx[0], ry[0])
    } else {
        Pair(rx[1], ry[1])
        }

    return Pair(Point2D(cx1, cy1), Point2D(cx2, cy2))
        }

fun locateLine2d(
    a: Float, b: Float, c: Float,                      // Line equation
    mx: Float, my: Float                               // Point of the line
): Line2DEquation {
    val ma = a
    val mb = b
    val mc = c - a * mx - b * my
    return Line2DEquation(ma, mb, mc)
    }

fun locateLine2d(
    dx: Float, dy: Float,                             // Line equation
    px: Float, py: Float                              // Point of the line
): Line2DEquation? {
    if (dx == 0.0f && dy == 0.0f)
        return null

    val ma = dy
    val mb = -dx
    val mc = py * dx - px * dy
    return Line2DEquation(ma, mb, mc)
    }

fun clip2d(
    x: Float, y: Float,                               // Coordinates of point
    lc: Float, rc: Float, tc: Float, bc: Float        // Corners: left, right, top, bottom
): Boolean {
    return x >= lc && x <= rc && y >= bc && y <= tc
    }
