import { ElMessageBox } from 'element-plus'

export const confirmAction = ({ title, message, impact, confirmButtonText, danger = false }) => ElMessageBox.confirm(
  `${message}\n\n${impact}`,
  title,
  {
    type: danger ? 'warning' : 'info',
    confirmButtonText,
    cancelButtonText: '取消',
    confirmButtonClass: danger ? 'el-button--danger' : '',
    distinguishCancelAndClose: true,
  },
)
