# Project Rules — Private Browser Android App

## Figma Configuration

Khi làm việc với Figma trong project này, sử dụng thông tin sau:

- **Figma Access Token**: đặt qua biến môi trường `FIGMA_ACCESS_TOKEN`
- **Figma File Key**: `q5XhpP4IcFGD2Y6bkEGujd`
- **File Name**: AM_Private Browser: Safe

### Cách sử dụng Token

**REST API (curl/wget):**
```bash
curl -s -H "X-Figma-Token: ${FIGMA_ACCESS_TOKEN}" \
  "https://api.figma.com/v1/files/q5XhpP4IcFGD2Y6bkEGujd/nodes?ids=<NODE_ID>"
```

**Export screenshot PNG:**
```bash
curl -s -H "X-Figma-Token: ${FIGMA_ACCESS_TOKEN}" \
  "https://api.figma.com/v1/images/q5XhpP4IcFGD2Y6bkEGujd?ids=<NODE_ID>&format=png&scale=2"
```

**Export SVG:**
```bash
curl -s -H "X-Figma-Token: ${FIGMA_ACCESS_TOKEN}" \
  "https://api.figma.com/v1/images/q5XhpP4IcFGD2Y6bkEGujd?ids=<NODE_ID>&format=svg"
```

> **Lưu ý:** Khi extract nodeId từ Figma URL, thay `-` bằng `:`. Ví dụ: `node-id=11145-8024` → nodeId = `11145:8024`
