const test = require('node:test');
const assert = require('node:assert/strict');
const { PNG } = require('pngjs');

const { compareImageBuffers } = require('../tests/query-explanation-visual-image.js');

test('pixel comparison treats identical decoded images as equal', async () => {
    const image = encodePng(2, 1, [0, 0, 0, 255, 120, 80, 40, 255]);

    const result = await compareImageBuffers(image, image);
    const diff = PNG.sync.read(result.diff);

    assert.equal(result.differentPixels, 0);
    assert.equal(diff.width, 2);
    assert.equal(diff.height, 1);
});

test('pixel comparison detects a one-channel change with no tolerance', async () => {
    const reference = encodePng(1, 1, [0, 0, 0, 255]);
    const actual = encodePng(1, 1, [1, 0, 0, 255]);

    const result = await compareImageBuffers(reference, actual);

    assert.equal(result.differentPixels, 1);
});

test('pixel comparison rejects images with different dimensions', async () => {
    const reference = encodePng(1, 1, [0, 0, 0, 255]);
    const actual = encodePng(2, 1, [0, 0, 0, 255, 0, 0, 0, 255]);

    await assert.rejects(
        compareImageBuffers(reference, actual),
        /Image dimensions differ: expected 1x1, got 2x1/
    );
});

function encodePng(width, height, pixels) {
    const image = new PNG({ width, height });
    image.data = Buffer.from(pixels);
    return PNG.sync.write(image);
}
