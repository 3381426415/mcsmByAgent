import { ElMessageBox } from 'element-plus'

/**
 * mac 风格的确认弹窗
 * @param {string} message 内容
 * @param {string} title 标题
 * @param {string} type 类型: 'warning' (红色按钮) | 'info' (蓝色按钮)
 */
export const appleConfirm = (message, title = '提示', type = 'warning') => {
  return ElMessageBox.confirm(message, title, {
    confirmButtonText: type === 'warning' ? '确认删除' : '确定',
    cancelButtonText: '取消',
    type: type,
    center: true, // 苹果风格通常居中
    draggable: true, // 允许拖拽，增加系统原生感
    customClass: 'apple-message-box', // 绑定你在全局 CSS 写的类名
    // 动态绑定确认按钮颜色：如果是警告类操作，强制用红色
    confirmButtonClass: type === 'warning' ? 'el-button--danger' : 'el-button--primary',
    cancelButtonClass: 'el-button--default',
    // 隐藏关闭小叉叉，让界面更简洁
    showClose: false,
  })
}

/**
 * 极简提醒弹窗 (Alert)
 */
export const appleAlert = (message, title = '通知') => {
  return ElMessageBox.alert(message, title, {
    confirmButtonText: '好',
    center: true,
    customClass: 'apple-message-box',
    showClose: false,
  })
}