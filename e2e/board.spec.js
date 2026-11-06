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
  expect(pieceBox.width).toBeLessThanOrEqual(squareBox.width);
  expect(pieceBox.height).toBeLessThanOrEqual(squareBox.height);
  await expect(firstPiece).toHaveJSProperty("draggable", false);
  await expect(firstPiece).toHaveCSS("pointer-events", "none");
});

