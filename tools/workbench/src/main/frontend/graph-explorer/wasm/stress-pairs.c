/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

/* Optional scalar f64 WebAssembly kernel. Port of graph-layout-stress.ts.
 * Keep operation order, xorshift calls, f32 hop storage, and immediate updates.
 * Build without fast math, floating-point contraction, or SIMD.
 */
#include <stdint.h>

static inline double next_random(uint32_t *state) {
    uint32_t s = *state;
    s ^= s << 13;
    s ^= s >> 17;
    s ^= s << 5;
    *state = s;
    return (double)s / 4294967296.0;
}

// NaN-propagating IEEE minimum, including the ordering of signed zero, like Math.min.
static inline double minimum(double a, double b) {
    return __builtin_elementwise_minimum(a, b);
}

__attribute__((export_name("shuffle")))
void shuffle(uint32_t *pairs, float *hops, uint32_t count, uint32_t *state) {
    for (uint32_t p = count; p > 1;) {
        --p;
        // The product is finite and nonnegative; truncation therefore equals Math.floor.
        const uint32_t q = (uint32_t)(next_random(state) * (double)(p + 1));
        const uint32_t t = pairs[p]; pairs[p] = pairs[q]; pairs[q] = t;
        const float h = hops[p]; hops[p] = hops[q]; hops[q] = h;
    }
}

__attribute__((export_name("pair_epoch")))
void pair_epoch(double *x, double *y, const uint8_t *fixed,
    const double *left, const double *right, const double *top, const double *bottom,
    const uint32_t *pairs, const float *hops, uint32_t count, double L,
    double typicalHalfWidth, double typicalHalfHeight, double linkWeight, double eta, uint32_t *state) {
    for (uint32_t k = 0; k < count; ++k) {
        const uint32_t pair = pairs[k], a = pair & 0xffff, b = pair >> 16;
        const uint8_t fa = fixed[a], fb = fixed[b];
        if (fa && fb) continue;
        const double d = hops[k];
        double dx = x[a] - x[b], dy = y[a] - y[b];
        double distance = __builtin_sqrt(dx * dx + dy * dy);
        if (distance < 1e-6) {
            dx = 1e-3 * (next_random(state) - 0.5);
            dy = 1e-3 * (next_random(state) - 0.5);
            distance = __builtin_sqrt(dx * dx + dy * dy);
        }
        const double ux = dx / distance, uy = dy / distance;
        const double ax = __builtin_fabs(ux), ay = __builtin_fabs(uy);
        const double eax = -ux >= 0 ? right[a] : left[a], eay = -uy >= 0 ? bottom[a] : top[a];
        const double ebx = ux >= 0 ? right[b] : left[b], eby = uy >= 0 ? bottom[b] : top[b];
        const double ra = ax < 1e-9 ? eay : ay < 1e-9 ? eax : minimum(eax / ax, eay / ay);
        const double rb = ax < 1e-9 ? eby : ay < 1e-9 ? ebx : minimum(ebx / ax, eby / ay);
        double target = ra + rb + d * L;
        if (d > 1) target += (d - 1) * 2 * (ax < 1e-9 ? typicalHalfHeight : ay < 1e-9 ? typicalHalfWidth : minimum(typicalHalfWidth / ax, typicalHalfHeight / ay));
        const double weight = (d <= 1 ? linkWeight : 1) / (target * target);
        const double mu = minimum(1, eta * weight);
        const double shift = mu * (distance - target) / 2;
        if (fa) { x[b] += 2 * shift * ux; y[b] += 2 * shift * uy; continue; }
        if (fb) { x[a] -= 2 * shift * ux; y[a] -= 2 * shift * uy; continue; }
        x[a] -= shift * ux; y[a] -= shift * uy;
        x[b] += shift * ux; y[b] += shift * uy;
    }
}

// One JS-to-Wasm call per full shuffle and sequential stress pass.
__attribute__((export_name("epoch")))
void epoch(double *x, double *y, const uint8_t *fixed,
    const double *left, const double *right, const double *top, const double *bottom,
    uint32_t *pairs, float *hops, uint32_t count, double L,
    double typicalHalfWidth, double typicalHalfHeight, double linkWeight, double eta, uint32_t *state) {
    shuffle(pairs, hops, count, state);
    pair_epoch(x, y, fixed, left, right, top, bottom, pairs, hops, count, L,
        typicalHalfWidth, typicalHalfHeight, linkWeight, eta, state);
}
