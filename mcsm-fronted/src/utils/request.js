import axios from 'axios';
import router from '@/router';
import { ElMessage } from 'element-plus';

// 根据环境自动切换 baseURL
const isDev = import.meta.env.DEV;  // Vite 提供的开发环境判断

const request = axios.create({
    baseURL: isDev ? 'http://127.0.0.1:8000' : '',
    timeout: 5000
});
let loadingCount = 0;

// 1. 请求拦截器
request.interceptors.request.use(config => {
    if (!config.silent) {
        showLoading();
    }
    const token = localStorage.getItem('token');
    if (token) {
        config.headers['token'] = token;
    }else{
        delete config.headers['token']
    }
    return config;
}, error => {
     hideLoading();
    return Promise.reject(error);
});

// 2. 响应拦截器 —— 不再全局弹窗，由各页面自行处理错误提示
request.interceptors.response.use(
    response => {
        if (!response.config?.silent) {
            hideLoading();
        }
        const res = response.data;

        if (res.code === 2000) {
            return res;
        }

        // 401 仍需全局处理：跳转登录页
        if (res.code === 401) {
            handleUnauthorized();
        }

        // 业务错误：携带 msg 供页面捕获
        return Promise.reject({ ...res, friendlyMsg: res.msg });
    },
    error => {
        if (!error.config?.silent) {
            hideLoading();
        }

        // 生成友好消息，附加到 error 上供页面使用
        let message = '';
        if (error.response) {
            const status = error.response.status;
            switch (status) {
                case 401:
                    handleUnauthorized();
                    message = '登录已过期';
                    break;
                case 403:
                    message = '权限不足';
                    break;
                case 404:
                    message = '请求资源不存在';
                    break;
                case 500:
                    message = '服务器内部错误';
                    break;
                default:
                    message = `网络异常 (${status})`;
            }
        } else if (error.request) {
            message = (error.code === 'ECONNABORTED' && error.message.indexOf('timeout') !== -1)
                ? '请求超时'
                : '无法连接到服务器';
        } else {
            message = '请求失败';
        }

        error.friendlyMsg = message;
        return Promise.reject(error);
    }
);

/**
 * 提取重复的登出逻辑
 */
function handleUnauthorized() {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('user_roles');
    localStorage.removeItem('user_permissions');
    // 避免重复跳转导致的路由冗余报错
    if (router.currentRoute.value.path !== '/') {
        router.push('/');
    }
}


function showLoading() {
    if (loadingCount === 0) {
        const el = document.createElement('div');
        el.id = '__global_loading';
        el.innerHTML = '<div class="global-spinner"></div>';
        document.body.appendChild(el);
    }
    loadingCount++;
}
function hideLoading() {
    loadingCount--;
    if (loadingCount <= 0) {
        loadingCount = 0;
        document.getElementById('__global_loading')?.remove();
    }
}


export default request;