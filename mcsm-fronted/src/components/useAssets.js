//useAssets.js

export function useAssets() {
  // 获取环境变量: http://192.168.10.6:9000/...
  const ASSETS_BASE = import.meta.env.VITE_APP_ASSETS_URL;

  const getIconUrl = (itemId) => {
    if (!itemId) return '';
    
    // 1. 清洗 ID: "minecraft:iron_ingot" -> "iron_ingot"
    const fileName = itemId.includes(':') ? itemId.split(':')[1] : itemId;
    
    // 2. 确保 Base 路径末尾有斜杠
    const safeBase = ASSETS_BASE.endsWith('/') ? ASSETS_BASE : `${ASSETS_BASE}/`;
    
    // 3. 拼接最终路径: 基础路径 + items目录 + 文件名.png
    // 结果: http://192.168.10.6:9000/static/items/iron_ingot.png
    
    return `${safeBase}item/${fileName}.png`;
  };



  return { getIconUrl };
}