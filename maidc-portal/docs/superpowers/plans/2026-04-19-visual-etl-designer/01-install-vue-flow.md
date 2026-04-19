# Task 01: Install @vue-flow/core

**Files:**
- Modify: `package.json` (via npm install)

- [ ] **Step 1: Install Vue Flow packages**

```bash
cd E:/pxg_project/maidc-portal
npm install @vue-flow/core @vue-flow/background @vue-flow/controls @vue-flow/minimap
```

- [ ] **Step 2: Verify installation**

```bash
npm ls @vue-flow/core
```

Expected: `@vue-flow/core@1.x.x` listed without errors

- [ ] **Step 3: Verify TypeScript support**

Vue Flow ships its own type definitions. Verify by checking:

```bash
ls node_modules/@vue-flow/core/dist/
```

Expected: `.d.ts` files present (e.g., `index.d.ts`)

- [ ] **Step 4: Commit**

```bash
git add package.json package-lock.json
git commit -m "chore: add @vue-flow/core + background + controls + minimap for visual ETL designer"
```
