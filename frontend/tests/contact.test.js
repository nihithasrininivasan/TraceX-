import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import {
  isValidEmail,
  isValidSender,
  validateReport,
} from "../src/utils/validation.js";
const cases = JSON.parse(
  readFileSync(
    new URL(
      "../../backend/src/test/resources/contact-format-cases.json",
      import.meta.url,
    ),
  ),
);
test("email and sender edge cases match the shared backend contract", () => {
  for (const [group, validate, expected] of [
    ["validEmails", isValidEmail, true],
    ["invalidEmails", isValidEmail, false],
    ["validSenders", isValidSender, true],
    ["invalidSenders", isValidSender, false],
  ])
    for (const value of cases[group])
      assert.equal(validate(value), expected, JSON.stringify(value));
  assert.equal(isValidEmail(`${"a".repeat(65)}@example.com`), false);
  assert.equal(isValidEmail(`a@${"b".repeat(64)}.com`), false);
  assert.equal(isValidSender(null), true);
});
test("report submission blocks malformed sender but permits no sender or URL", () => {
  const report = {
    title: "Phone scam",
    description: "Caller requested my student account password.",
  };
  assert.equal(validateReport(report), "");
  assert.equal(validateReport({ ...report, sender: "+91 98765 43210" }), "");
  assert.match(
    validateReport({ ...report, sender: "not an email" }),
    /valid sender/,
  );
});
