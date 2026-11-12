const { test, expect } = require('@playwright/test');

test('approved club design: illustrated cards, targeting, navigation and responsive geometry', async ({ page, isMobile }, info) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.setViewportSize(isMobile ? { width: 390, height: 844 } : { width: 1440, height: 1000 });
  await page.goto('/');
  await page.locator('[data-auth-mode="register"]').click();
  const username = 'design' + Date.now().toString(36);
  await page.locator('#username').fill(username);
  await page.locator('#email').fill(username + '@example.com');
  await page.locator('#password').fill('DesignTest123!');
  await page.locator('#auth-submit').click();
  await expect(page.locator('#app-view')).toBeVisible();
  await page.locator('[data-route="collection"]').click();
  for (const name of ['Healing Spring', 'Freeze', 'Arcane Strike']) {
    const tile = page.locator('#shop-items .card-tile').filter({ has: page.getByRole('heading', { name, exact: true }) });
    await tile.getByRole('button').click();
    await expect(page.locator('#card-inventory')).toContainText(name);
  }
  await page.locator('[data-route="home"]').click();
  await page.locator('#pve-difficulty').selectOption('HARD');
  await page.locator('[data-create-game="PVE"]').click();
  await expect(page.locator('#board')).toHaveAttribute('aria-busy', 'false');
  await expect(page.locator('.tactical-card .card-art')).toHaveCount(3);
  await expect(page.locator('#white-name')).toHaveText('Bạn');
  await expect(page.locator('#black-name')).toHaveText('Bot · Khó');
  const freeze = page.locator('.tactical-card').filter({ hasText: 'Freeze' });
  await freeze.focus();
  await page.keyboard.press('Enter');
  await expect(page.locator('#card-target')).toContainText('Chọn một quân đối thủ');
  await expect(freeze).toHaveAttribute('aria-pressed', 'true');
  await page.locator('#cancel-card').click();
  await expect(page.locator('#card-target')).toBeHidden();
  await freeze.click();
  await page.evaluate(async () => {
    await document.fonts.ready;
    const image = new Image();
    image.src = '/assets/art/arcane-cards.png';
    await image.decode();
    if (!document.fonts.check('14px "Be Vietnam Pro"')) throw new Error('Vietnamese font failed');
  });
  const board = await page.locator('#board').boundingBox();
  expect(Math.abs(board.width - board.height)).toBeLessThan(1);
  const sizes = await page.locator('#board .square').evaluateAll(cells => cells.map(cell => ({ w: cell.offsetWidth, h: cell.offsetHeight })));
  expect(sizes).toHaveLength(64);
  sizes.forEach(cell => expect(Math.abs(cell.w - cell.h)).toBeLessThanOrEqual(1));
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  if (isMobile) {
    const nav = await page.locator('.main-nav').boundingBox();
    const viewport = page.viewportSize();
    expect(nav.y + nav.height).toBeLessThanOrEqual(viewport.height + 1);
    expect(nav.height).toBeGreaterThanOrEqual(56);
    const cards = await page.locator('.tactical-card').all();
    const boxes = await Promise.all(cards.map(card => card.boundingBox()));
    expect(Math.max(...boxes.map(box => box.y)) - Math.min(...boxes.map(box => box.y))).toBeLessThan(1);
    for (const box of boxes) expect(box.y + box.height).toBeLessThan(nav.y);
    const target = await page.locator('#card-target').boundingBox();
    expect(target.y + target.height).toBeLessThan(nav.y);
  }
  await page.evaluate(() => window.scrollTo(0, 0));
  await expect(page.locator('#toast')).not.toHaveClass(/show/, { timeout: 5000 });
  await page.screenshot({ path: info.outputPath('club-design.png'), fullPage: true, animations: 'disabled' });
  await page.screenshot({ path: info.outputPath('club-viewport.png'), animations: 'disabled' });
  await page.locator('#cancel-card').click();
  if (isMobile) await page.locator('[data-route="home"]').click();
  else await page.locator('[data-choose-mode="LOCAL"]').click();
  await expect(page.locator('[data-page="home"]')).toBeVisible();
  expect(errors).toEqual([]);
});
