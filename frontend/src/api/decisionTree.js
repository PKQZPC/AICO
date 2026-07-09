import axios from 'axios';

// 直接使用后端URL，不通过代理
const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
  },
  timeout: 5000, // 添加超时设置
  validateStatus: function (status) {
    return status >= 200 && status < 300; // 只接受 2xx 状态码
  }
});

// 添加请求拦截器
apiClient.interceptors.request.use(
  config => {
    console.log('发送请求:', config.url);
    return config;
  },
  error => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

// 添加响应拦截器
apiClient.interceptors.response.use(
  response => {
    console.log('收到响应:', response.status);
    return response;
  },
  error => {
    console.error('响应错误:', error);
    if (error.code === 'ECONNABORTED') {
      console.warn('请求超时');
    }
    return Promise.reject(error);
  }
);

// 模拟决策树数据，当API不可用时使用
const mockDecisionTree = {
  "tree_id": "from_backend_example",
  "tree_name": "后端样例决策树(模拟)",
  "root": {
    "node_id": "backend_root",
    "node_type": "decision",
    "label": "后端示例根节点"
  },
  "node_registry": [
    {
      "node_id": "backend_root",
      "node_type": "decision",
      "label": "后端示例根节点",
      "status": "confirmed",
      "node_content": "这是一个后端示例决策树的根节点",
      "children": [
        {
          "relationship": "solid",
          "edge_label": "选项1",
          "node_id": "backend_child1"
        },
        {
          "relationship": "dashed",
          "edge_label": "选项2",
          "node_id": "backend_child2"
        }
      ]
    },
    {
      "node_id": "backend_child1",
      "node_type": "leaf",
      "label": "后端子节点1",
      "node_content": "这是后端示例决策树的子节点1",
      "response_template": "这是后端示例决策树的响应1"
    },
    {
      "node_id": "backend_child2",
      "node_type": "leaf",
      "label": "后端子节点2",
      "node_content": "这是后端示例决策树的子节点2",
      "response_template": "这是后端示例决策树的响应2"
    }
  ],
  "metadata": {
    "version": "1.0.0",
    "generate_time": "2025-04-11T00:00:00+08:00"
  }
};

export const decisionTreeApi = {
  // 使用JSONP方式请求数据
  createJsonpRequest(url, callbackName = 'jsonpCallback') {
    return new Promise((resolve, reject) => {
      // 创建script标签
      const script = document.createElement('script');
      // 设置回调函数
      window[callbackName] = (data) => {
        resolve(data);
        document.body.removeChild(script);
        delete window[callbackName];
      };
      // 设置错误处理
      script.onerror = () => {
        reject(new Error('JSONP请求失败'));
        document.body.removeChild(script);
        delete window[callbackName];
      };
      // 设置超时
      const timeout = setTimeout(() => {
        reject(new Error('JSONP请求超时'));
        document.body.removeChild(script);
        delete window[callbackName];
      }, 10000);
      
      // 拼接URL
      script.src = `${url}${url.includes('?') ? '&' : '?'}callback=${callbackName}`;
      // 添加到DOM
      document.body.appendChild(script);
    });
  },
  
  // 检查后端连接状态
  async checkBackendConnection() {
    const statusResult = {
      connected: false,
      method: null,
      message: '未能连接到后端',
      error: null,
      timestamp: new Date().toISOString()
    };
    
    try {
      // 1. 尝试直接使用axios
      try {
        console.log('尝试直接连接后端...');
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 8000);
        
        const response = await apiClient.get('/decision_trees', {
          signal: controller.signal
        });
        
        clearTimeout(timeoutId);
        
        // 添加更详细的调试信息
        console.log('收到后端响应:', {
          status: response.status,
          statusText: response.statusText,
          headers: response.headers,
          dataType: typeof response.data,
          dataPreview: typeof response.data === 'object' ? 
            JSON.stringify(response.data).substring(0, 100) + '...' : 
            String(response.data).substring(0, 100) + '...'
        });
        
        if (response.status >= 200 && response.status < 300) {
          return {
            connected: true,
            method: 'direct',
            message: '成功连接到后端',
            data: response.data,
            timestamp: new Date().toISOString()
          };
        } else {
          console.warn(`收到非成功状态码: ${response.status}`);
        }
      } catch (axiosError) {
        console.warn('直接连接后端失败:', axiosError);
        console.log('错误详情:', {
          message: axiosError.message,
          code: axiosError.code,
          response: axiosError.response ? {
            status: axiosError.response.status,
            statusText: axiosError.response.statusText,
            data: axiosError.response.data
          } : 'No response',
          request: axiosError.request ? '请求已发送但无响应' : '请求未发送'
        });
        
        statusResult.error = axiosError.message;
        
        // 2. 尝试fetch请求
        try {
          console.log('尝试使用fetch请求后端...');
          const controller = new AbortController();
          const timeoutId = setTimeout(() => controller.abort(), 8000);
          
          const response = await fetch('/api/decision_trees', {
            method: 'GET',
            headers: {
              'Accept': 'application/json'
            },
            mode: 'cors',
            signal: controller.signal
          });
          
          clearTimeout(timeoutId);
          
          if (response.ok) {
            return {
              connected: true,
              method: 'fetch',
              message: '成功通过fetch连接到后端',
              timestamp: new Date().toISOString()
            };
          }
        } catch (fetchError) {
          console.warn('fetch请求后端失败:', fetchError);
          statusResult.error = `${statusResult.error}; ${fetchError.message}`;
          
          // 3. 尝试JSONP
          try {
            console.log('尝试JSONP连接后端...');
            const jsonpData = await this.createJsonpRequest('/api/decision_trees');
            
            if (jsonpData) {
              return {
                connected: true,
                method: 'jsonp',
                message: '成功通过JSONP连接到后端',
                data: jsonpData,
                timestamp: new Date().toISOString()
              };
            }
          } catch (jsonpError) {
            console.warn('JSONP连接后端失败:', jsonpError);
            statusResult.error = `${statusResult.error}; ${jsonpError.message}`;
          }
        }
      }
      
      // 所有方法都失败
      return statusResult;
    } catch (error) {
      console.error('检测后端连接失败:', error);
      return {
        connected: false,
        method: null,
        message: '检测后端连接时发生错误',
        error: error.message,
        timestamp: new Date().toISOString()
      };
    }
  },

  // 获取决策树列表
  async getDecisionTrees() {
    try {
      console.log('请求决策树列表...');
      
      // 尝试多种方式获取数据
      try {
        // 1. 首先使用axios直接请求
        console.log('发送axios请求到:', apiClient.defaults.baseURL + '/decision_trees');
        const response = await apiClient.get('/decision_trees');
        
        // 添加详细的响应调试
        console.log('axios请求成功，状态码:', response.status);
        console.log('响应头:', response.headers);
        console.log('数据类型:', typeof response.data);
        console.log('数据是否为数组:', Array.isArray(response.data));
        
        if (Array.isArray(response.data)) {
          console.log('数组长度:', response.data.length);
          if (response.data.length > 0) {
            console.log('第一项示例:', JSON.stringify(response.data[0]).substring(0, 200) + '...');
          }
        } else if (typeof response.data === 'object') {
          console.log('对象键:', Object.keys(response.data));
          console.log('对象预览:', JSON.stringify(response.data).substring(0, 200) + '...');
        } else {
          console.log('数据预览:', String(response.data).substring(0, 200) + '...');
        }
        
        console.log('通过axios获取决策树成功');
        return response.data;
      } catch (axiosError) {
        console.warn('通过axios获取决策树失败，尝试fetch请求:', axiosError);
        console.log('axios错误详情:', {
          message: axiosError.message,
          code: axiosError.code,
          response: axiosError.response ? {
            status: axiosError.response.status,
            statusText: axiosError.response.statusText,
            data: axiosError.response.data
          } : 'No response'
        });
        
        try {
          // 2. 尝试使用fetch请求
          console.log('发送fetch请求到: /api/decision_trees');
          const response = await fetch('/api/decision_trees', {
            method: 'GET',
            headers: {
              'Accept': 'application/json',
              'Content-Type': 'application/json'
            },
            mode: 'cors' // 允许跨域
          });
          
          console.log('fetch请求响应状态:', response.status, response.statusText);
          console.log('fetch响应头:', {
            type: response.type,
            contentType: response.headers.get('content-type')
          });
          
          if (!response.ok) {
            throw new Error(`状态码: ${response.status}`);
          }
          
          const data = await response.json();
          console.log('fetch响应数据类型:', typeof data);
          console.log('fetch响应是否为数组:', Array.isArray(data));
          console.log('fetch数据预览:', JSON.stringify(data).substring(0, 200) + '...');
          return data;
        } catch (fetchError) {
          console.warn('fetch请求获取决策树失败，尝试JSONP:', fetchError);
          
          try {
            // 3. 尝试JSONP请求
            const data = await this.createJsonpRequest('/api/decision_trees');
            console.log('通过JSONP获取决策树成功:', data);
            return data;
          } catch (jsonpError) {
            console.warn('所有请求方式均失败，使用模拟数据:', jsonpError);
            
            // 4. 使用模拟数据
            console.log('使用模拟决策树数据');
            return [mockDecisionTree];
          }
        }
      }
    } catch (error) {
      console.error('获取决策树失败:', error);
      // 返回模拟数据
      return [mockDecisionTree];
    }
  },

  // 获取特定决策树
  async getDecisionTreeById(treeId) {
    try {
      console.log(`请求决策树 ${treeId}...`);
      
      // 检查是否是模拟数据ID
      if (treeId === 'from_backend_example') {
        console.log('返回模拟决策树数据');
        return mockDecisionTree;
      }
      
      // 检查是否是静态树ID
      if (treeId === 'anxiety_management_v1' || treeId === 'stress' || treeId === 'homework') {
        console.log('使用静态决策树数据');
        return staticTrees[treeId];
      }
      
      try {
        // 1. 首先通过axios获取
        const response = await apiClient.get(`/decision_trees/${treeId}`);
        console.log(`通过axios获取决策树 ${treeId} 成功:`, response.data);
        return response.data;
      } catch (axiosError) {
        console.warn(`通过axios获取决策树 ${treeId} 失败:`, axiosError);
        
        try {
          // 2. 尝试通过fetch获取
          const response = await fetch(`/api/decision_trees/${treeId}`, {
            method: 'GET',
            headers: {
              'Accept': 'application/json'
            },
            mode: 'cors'
          });
          
          if (!response.ok) {
            throw new Error(`状态码: ${response.status}`);
          }
          
          const data = await response.json();
          return data;
        } catch (fetchError) {
          console.warn(`通过fetch获取决策树 ${treeId} 失败:`, fetchError);
          
          // 3. 尝试从已缓存的后端树中查找
          const backendTree = this.backendTrees?.find(tree => tree.tree_id === treeId);
          if (backendTree) {
            console.log('使用已缓存的后端树:', treeId);
            return backendTree;
          }
          
          // 4. 尝试从静态数据中查找
          if (staticTrees[treeId]) {
            console.log('使用静态决策树数据:', treeId);
            return staticTrees[treeId];
          }
          
          // 5. 如果都找不到，返回模拟数据
          console.warn(`无法找到决策树 ${treeId}，返回模拟数据`);
          return mockDecisionTree;
        }
      }
    } catch (error) {
      console.error(`获取决策树 ${treeId} 失败:`, error);
      
      // 尝试从静态数据中查找
      if (staticTrees[treeId]) {
        console.log('使用静态决策树数据:', treeId);
        return staticTrees[treeId];
      }
      
      // 如果都找不到，返回模拟数据
      console.warn(`无法找到决策树 ${treeId}，返回模拟数据`);
      return mockDecisionTree;
    }
  },

  // 更新决策树
  async updateDecisionTree(treeId, treeData, expertStats) {
    try {
      console.log(`更新决策树 ${treeId}...`);
      
      const payload = {
        tree: treeData,
        expertDecisionTreeExport: expertStats
      };
      
      const response = await apiClient.put(`/decision_trees/${treeId}`, payload);
      console.log(`更新决策树 ${treeId} 成功:`, response.data);
      return response.data;
    } catch (error) {
      console.error(`更新决策树 ${treeId} 失败:`, error);
      throw error;
    }
  }
}; 