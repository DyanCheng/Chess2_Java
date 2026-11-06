const { test, expect } = require("@playwright/test");

test("board is an 8 by 8 square and pieces cannot be dragged", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "Chơi nhanh với tài khoản khách" }).click();
  await expect(page.locator("#app-view")).toBeVisible();
  await page.getByRole("button", { name: "Bắt đầu PvE" }).click();
  const board = page.locator("#board");
  await expect(board).toBeVisible();
  await expect(board.locator(".square")).toHaveCount(64);

  const boardBox = await board.boundingBox();
  expect(boardBox).not.toBeNull();
  expect(Math.abs(boardBox.width - boardBox.height)).toBeLessThanOrEqual(1);

  const firstSquare = board.locator(".square").first();
  const firstPiece = board.locator(".piece").first();
  const squareBox = await firstSquare.boundingBox();
  const pieceBox = await firstPiece.boundingBox();
  expect(squareBox).not.toBeNull();
  expect(pieceBox).not.toBeNull();
  expect(Math.abs(squareBox.width - squareBox.height)).toBeLessThanOrEqual(1);
  expect(pieceBox.width).toBeLessThanOrEqual(squareBox.width + 0.1);
  expect(pieceBox.height).toBeLessThanOrEqual(squareBox.height + 0.1);
  await expect(firstPiece).toHaveJSProperty("draggable", false);
  await expect(firstPiece).toHaveCSS("pointer-events", "none");
});

test("a player can drag a piece to a legal square without native image dragging", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "Chơi nhanh với tài khoản khách" }).click();
  await page.getByRole("button", { name: "Bắt đầu PvE" }).click();
  await expect(page.locator("#game-status")).toHaveText("Lượt Trắng.");
  const board = page.locator("#board");
  await expect(board).toBeVisible();
  const from = board.locator('[data-square="a2"]');
  const to = board.locator('[data-square="a3"]');
  const fromBox = await from.boundingBox();
  const toBox = await to.boundingBox();
  expect(fromBox).not.toBeNull();
  expect(toBox).not.toBeNull();
  await page.mouse.move(fromBox.x + fromBox.width / 2, fromBox.y + fromBox.height / 2);
  await page.mouse.down();
  await page.mouse.move(toBox.x + toBox.width / 2, toBox.y + toBox.height / 2, { steps: 8 });
  await expect(board).toHaveClass(/is-dragging/);
  await page.mouse.up();
  await expect(page.locator(".chess-drag-ghost")).toHaveCount(0);
  await expect(board.locator('[data-square="a3"] .piece')).toHaveText("♙");
  await expect(board.locator('[data-square="a2"] .piece')).toHaveCount(0);
});

