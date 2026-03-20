async function typeIntoCodeMirror(page, index, value) {
    await page.locator('.CodeMirror').nth(index).click();
    await page.keyboard.press('ControlOrMeta+A');
    await page.keyboard.press('Backspace');
    await page.keyboard.type(value);
    await page.waitForFunction(([editorIndex, expectedValue]) => {
        return document.querySelectorAll('.CodeMirror')[editorIndex].CodeMirror.getValue() === expectedValue;
    }, [index, value]);
}

module.exports = {
    typeIntoCodeMirror
};
