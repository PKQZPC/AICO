<!-- src/views/DecisionTree.vue -->
<template>
  <div class="decision-tree-container">
    <!-- 顶部导航栏 -->
    <div class="header">
      <h2>专家协助平台</h2>
      <div class="header-actions">
        <el-button class="btn-meeting" @click="navigateToExpertChat" type="primary" plain>
          <i class="icon-meeting"></i>
          会话界面
        </el-button>
        <el-button class="btn-voice" @click="handleVisualization" type="primary">
          <i class="icon-voice"></i>
          决策树可视化
        </el-button>
        <el-button class="btn-refresh" @click="refreshBackendTrees" :disabled="isRefreshing" type="info">
          <i class="icon-refresh"></i>
          {{ isRefreshing ? '加载中...' : '刷新后端决策树' }}
        </el-button>
        <span v-if="connectionStatus" :class="`status-indicator ${connectionStatus.type}`">
          {{ connectionStatus.message }}
        </span>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧导航 -->
      <div class="sidebar">
        <div class="sidebar-title">场景列表</div>
        <ul class="menu-list">
          <!-- 静态预设决策树 -->
          <li class="menu-item" :class="{ active: treeStore.currentTreeId === 'anxiety_management_v1' }" @click="selectTree('anxiety_management_v1')">焦点管理决策树</li>
          <li class="menu-item" :class="{ active: treeStore.currentTreeId === 'stress' }" @click="selectTree('stress')">大学生/考生压力咨询</li>
          <li class="menu-item" :class="{ active: treeStore.currentTreeId === 'homework' }" @click="selectTree('homework')">测试决策树</li>
          
          <!-- 后端获取的决策树 -->
          <template v-if="treeStore.backendTrees.length > 0">
            <li class="menu-category">后端决策树</li>
            <li 
              v-for="tree in treeStore.backendTrees" 
              :key="tree.tree_id"
              class="menu-item" 
              :class="{ active: treeStore.currentTreeId === tree.tree_id }"
              @click="selectTree(tree.tree_id)"
            >
              {{ tree.tree_name || tree.tree_id }}
            </li>
          </template>
        </ul>
      </div>

      <!-- 右侧内容区域 -->
      <div class="content-wrapper">
        <div class="tree-title">{{ treeStore.currentTree?.tree_name || '焦点管理决策树' }}</div>
        
        <div class="content-area">
          <!-- 左侧决策树区域 -->
          <div class="decision-area">
            <div class="tree-container">
              <!-- 加载状态 -->
              <div v-if="loading" class="loading-container">
                <div class="loading-spinner"></div>
                <p>正在加载决策树...</p>
              </div>
              
              <!-- Vue Flow 组件 -->
              <div v-else class="flow-container">
                <VueFlow
                  v-model="elements"
                  :default-zoom="0.9"
                  :min-zoom="0.2"
                  :max-zoom="2"
                  :node-types="nodeTypes"
                  :default-viewport="{ x: 0, y: 0, zoom: 0.9 }"
                  class="flow-canvas"
                  @nodeClick="onNodeClick"
                  @nodesChange="onNodesChange"
                  @edgesChange="onEdgesChange"
                  @connect="handleConnect"
                >
                  <template #node-decision="nodeProps">
                    <DecisionNode 
                      v-bind="nodeProps" 
                      :display-label="nodeProps.data.displayLabel" 
                      :full-label="nodeProps.data.label" 
                    />
                  </template>
                  
                  <template #node-leaf="nodeProps">
                    <LeafNode 
                      v-bind="nodeProps" 
                      :display-label="nodeProps.data.displayLabel" 
                      :full-label="nodeProps.data.label" 
                    />
                  </template>
                  
                  <Background pattern-color="#aaa" :gap="16" />
                  <MiniMap />
                  <Controls />
                </VueFlow>
              </div>
              
              <!-- 图例部分 -->
              <div class="tree-legend">
                <div class="legend-item">
                  <span class="legend-label">图例:</span>
                  <span class="legend-dot decision"></span>
                  <span class="legend-text">决策节点</span>
                  <span class="legend-dot leaf"></span>
                  <span class="legend-text">叶子节点</span>
                  <span class="legend-line solid"></span>
                  <span class="legend-text">确认关系</span>
                  <span class="legend-line dashed"></span>
                  <span class="legend-text">待确认关系</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 右侧节点内容面板 -->
          <div class="node-area">
            <div class="node-panel">
              <h3 class="panel-title">节点详情</h3>
              <p class="panel-desc">您可以查看并编辑节点信息</p>
              
              <!-- 面板操作按钮 -->
              <div class="panel-actions">
                <el-button class="btn-export" @click="exportModifications" type="success">导出修改数据</el-button>
                <el-button class="btn-clear" @click="clearModifications" type="warning">清除修改记录</el-button>
              </div>

              <!-- 节点管理按钮 -->
              <div class="node-management">
                <el-button class="btn-add" @click="showAddNodeDialog" type="success">新增节点</el-button>
                <el-button class="btn-delete" @click="deleteSelectedNode" :disabled="!selectedNode" type="danger">删除节点</el-button>
              </div>
              
              <!-- 新增节点对话框 -->
              <el-dialog
                v-model="showAddNodeDialogFlag"
                title="新增节点"
                width="500px"
                :close-on-click-modal="false"
              >
                <el-form :model="newNodeForm" label-width="100px">
                  <el-form-item label="节点类型">
                    <el-select v-model="newNodeForm.node_type" placeholder="请选择节点类型">
                      <el-option label="决策节点" value="decision" />
                      <el-option label="叶子节点" value="leaf" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="节点名称">
                    <el-input v-model="newNodeForm.label" placeholder="请输入节点名称" />
                  </el-form-item>
                  <el-form-item label="节点内容">
                    <el-input
                      v-model="newNodeForm.node_content"
                      type="textarea"
                      rows="3"
                      placeholder="请输入节点内容"
                    />
                  </el-form-item>
                  <el-form-item label="父节点">
                    <el-select v-model="newNodeForm.parent_id" placeholder="请选择父节点">
                      <el-option
                        v-for="node in availableParentNodes"
                        :key="node.id"
                        :label="node.data.label"
                        :value="node.id"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="关系类型">
                    <el-select v-model="newNodeForm.relationship" placeholder="请选择关系类型">
                      <el-option label="确认关系" value="solid" />
                      <el-option label="待确认关系" value="dashed" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="关系标签">
                    <el-input v-model="newNodeForm.edge_label" placeholder="请输入关系标签" />
                  </el-form-item>
                </el-form>
                <template #footer>
                  <span class="dialog-footer">
                    <el-button @click="showAddNodeDialogFlag = false">取消</el-button>
                    <el-button type="primary" @click="addNewNode">确定</el-button>
                  </span>
                </template>
              </el-dialog>
              
              <div v-if="selectedNode" class="node-content">
                <!-- 添加节点名称编辑 -->
                <div class="content-section">
                  <h5>节点名称</h5>
                  <div class="edit-field">
                    <input 
                      v-model="editedNode.label" 
                      class="form-control" 
                      placeholder="请输入节点名称"
                    />
                  </div>
                </div>
                
                <!-- 节点状态 -->
                <div class="content-section">
                  <h5>节点状态</h5>
                  <div class="edit-field">
                    <select v-model="editedNode.status" class="form-control">
                      <option value="to_be_confirm">待确认</option>
                      <option value="confirmed">已确认</option>
                      <option value="rejected">已拒绝</option>
                    </select>
                  </div>
                </div>
                
                <!-- 节点内容 -->
                <div class="content-section">
                  <h5>节点内容</h5>
                  <div class="edit-field">
                    <textarea 
                      v-model="editedNode.node_content" 
                      class="form-control" 
                      rows="3"
                      placeholder="请输入节点内容"
                    ></textarea>
                  </div>
                </div>
                
                <!-- 上下文内容 -->
                <div class="content-section">
                  <h5>上下文内容</h5>
                  <div class="edit-field">
                    <textarea 
                      v-model="editedNode.source_content" 
                      class="form-control" 
                      rows="3"
                      placeholder="请输入上下文内容"
                    ></textarea>
                  </div>
                </div>
                
                <!-- 推理关系 -->
                <div class="content-section">
                  <h5>推理关系</h5>
                  <div class="edit-field">
                    <textarea 
                      v-model="editedNode.inference_relationship" 
                      class="form-control" 
                      rows="3"
                      placeholder="请描述推理关系"
                    ></textarea>
                  </div>
                </div>
                
                <!-- 提问/回复模板，根据节点类型显示 -->
                <div v-if="selectedNode.node_type === 'decision'" class="content-section">
                  <h5>提问模板</h5>
                  <div class="edit-field">
                    <textarea 
                      v-model="editedNode.prompt_template" 
                      class="form-control" 
                      rows="3"
                      placeholder="请输入提问模板"
                    ></textarea>
                  </div>
                </div>
                
                <div v-if="selectedNode.node_type === 'leaf'" class="content-section">
                  <h5>回复模板</h5>
                  <div class="edit-field">
                    <textarea 
                      v-model="editedNode.response_template" 
                      class="form-control" 
                      rows="3"
                      placeholder="请输入回复模板"
                    ></textarea>
                  </div>
                </div>
                
                <!-- 操作按钮 -->
                <div class="action-buttons">
                  <el-button class="btn-save" @click="saveNodeChanges" type="primary">保存修改</el-button>
                  <el-button class="btn-cancel" @click="cancelEdit">取消</el-button>
                </div>
              </div>
              
              <div v-else class="no-node-selected">
                请点击左侧决策树节点查看并编辑详细内容
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { VueFlow, useVueFlow } from '@vue-flow/core';
import '@vue-flow/core/dist/style.css';
import '@vue-flow/core/dist/theme-default.css';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import { MiniMap } from '@vue-flow/minimap';
import Elk from 'elkjs';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElButton } from 'element-plus';
import axios from 'axios';
import { decisionTreeApi } from '@/api/decisionTree';
import { useDecisionTreeStore, convertBackendTree } from '@/stores/decisionTree';

// 导入自定义节点组件
import DecisionNode from '@/components/DecisionNode.vue';
import LeafNode from '@/components/LeafNode.vue';

const router = useRouter();
const route = useRoute();
const treeStore = useDecisionTreeStore();
const loading = ref(false);
const isRefreshing = ref(false);
const connectionStatus = ref(null);
const elements = ref([]);  // 改回数组形式
const { fitView, setNodes, setEdges, onNodesChange, onEdgesChange, onConnect: handleConnect } = useVueFlow({
  defaultEdgeOptions: {
    type: 'step',
    style: {
      strokeWidth: 2,
      radius: 20
    },
    markerEnd: {
      type: 'arrow',
      width: 20,
      height: 20,
      color: '#1890ff'
    }
  },
  handleConnect: (params) => {
    return addEdge(params, edges.value)
  },
});

// 注册自定义节点类型
const nodeTypes = {
  decision: DecisionNode,
  leaf: LeafNode
};

// 替换变量
const replaceVariables = (text, variables) => {
  if (!text) return ''; // 如果text为空，返回空字符串
  if (!variables) return text; // 如果没有变量，直接返回原文本
  return text.replace(/\{(\w+)\}/g, (match, key) => variables[key] || match);
};

// 转换数据格式为Vue Flow可用的格式
const transformTree = (treeData) => {
  if (!treeData) return { nodes: [], edges: [] };
  
  const nodes = treeData.node_registry.map(node => ({
    id: node.node_id,
    type: node.node_type || 'decision',
    position: { x: 0, y: 0 }, // 初始位置，后续布局算法会重新计算
    data: {
      ...node,
      // 处理可能的模板变量替换，添加空值检查
      label: node.label ? replaceVariables(node.label, treeData.metadata?.variables) : '',
      // 添加显示用的短标签，超过10个字就截断
      displayLabel: node.label ? 
        (node.label.length > 10 ? 
          replaceVariables(node.label.substring(0, 10), treeData.metadata?.variables) + '...' : 
          replaceVariables(node.label, treeData.metadata?.variables)) : 
        '',
      node_content: node.node_content ? replaceVariables(node.node_content, treeData.metadata?.variables) : '',
      prompt_template: node.prompt_template ? replaceVariables(node.prompt_template, treeData.metadata?.variables) : '',
      response_template: node.response_template ? replaceVariables(node.response_template, treeData.metadata?.variables) : '',
    }
  }));
  
  const edges = [];
  treeData.node_registry.forEach(node => {
    if (node.children && node.children.length > 0) {
      node.children.forEach(child => {
        // 查找子节点的完整信息
        const childNode = treeData.node_registry.find(n => n.node_id === child.node_id);
        // 如果子节点状态为rejected，则不创建连线
        if (childNode && childNode.status === 'rejected') {
          return;
        }
        // 根据子节点状态决定连线样式
        const isChildPending = childNode && childNode.status === 'to_be_confirm';
        edges.push({
          id: `${node.node_id}-${child.node_id}`,
          source: node.node_id,
          target: child.node_id,
          label: child.edge_label || '',
          style: {
            strokeDasharray: isChildPending ? '5 5' : 'none',
            stroke: isChildPending ? '#f56c6c' : '#1890ff',
            strokeWidth: 2,
            radius: 20 // 增加边缘半径
          },
          type: 'step', // 使用step而不是smoothstep，减少交叉
          animated: isChildPending,
          markerEnd: {
            type: 'arrow',
            width: 20,
            height: 20,
            color: isChildPending ? '#f56c6c' : '#1890ff'
          }
        });
      });
    }
  });
  
  return { nodes, edges };
};

// 使用elkjs进行自动布局
const applyAutoLayout = async (elements) => {
  console.log('开始应用自动布局...', elements);
  const elk = new Elk({
    defaultLayoutOptions: {
      'elk.algorithm': 'layered',
      'elk.direction': 'DOWN',
      'elk.layered.spacing.nodeNodeBetweenLayers': 180, // 层间距更大
      'elk.spacing.nodeNode': 100, // 同级节点间距
      'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP', 
      'elk.layered.nodePlacement.strategy': 'NETWORK_SIMPLEX',
      'elk.layered.layering.strategy': 'NETWORK_SIMPLEX',
      'elk.layered.spacing.edgeNodeBetweenLayers': 60,
      'elk.layered.spacing.edgeEdgeBetweenLayers': 60,
      'elk.layered.spacing.edgeNode': 60,
      'elk.layered.spacing.edgeEdge': 60,
      // 增强边缘路径优化，避免交叉
      'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
      'elk.layered.crossingMinimization.forceNodeModelOrder': true,
      'elk.layered.edgeRouting': 'ORTHOGONAL',
      'elk.layered.mergeEdges': true,
      // 确保父节点在子节点上方
      'elk.hierarchyHandling': 'INCLUDE_CHILDREN',
      'elk.separateConnectedComponents': true,
      // 优化同级节点的水平布局
      'elk.alignment': 'CENTER',
      'elk.layered.nodePlacement.favorStraightEdges': true,
      'elk.layered.nodePlacement.bk.fixedAlignment': 'BALANCED',
      'elk.layered.thoroughness': 7, // 增加算法的彻底性，提高布局质量
      'elk.layered.nodePlacement.bk.edgeStraightening': 'IMPROVE_STRAIGHTNESS',
      // 额外优化以减少边交叉
      'elk.layered.cycleBreaking.strategy': 'DEPTH_FIRST',
      'elk.layered.crossMin': true,
      'elk.aspectRatio': 2.0
    }
  });
  
  // 构建层次化结构，处理同级节点
  const buildHierarchicalGraph = (nodes, edges) => {
    // 创建节点映射和层级结构
    const nodeMap = new Map();
    const levelMap = new Map(); // 记录节点层级
    
    // 初始化节点映射
    nodes.forEach(node => {
      nodeMap.set(node.id, { 
        ...node, 
        children: [], 
        parents: [], 
        level: 0 
      });
    });
    
    // 构建父子关系
    edges.forEach(edge => {
      const sourceNode = nodeMap.get(edge.source);
      const targetNode = nodeMap.get(edge.target);
      if (sourceNode && targetNode) {
        // 记录子节点，确保父节点在上方
        sourceNode.children.push(targetNode.id);
        // 记录父节点，用于计算层级
        targetNode.parents.push(sourceNode.id);
      }
    });
    
    // 计算节点层级（根节点层级为0，每下一层+1）
    const calculateLevels = () => {
      // 找出所有根节点（没有父节点的节点）
      const rootNodes = Array.from(nodeMap.values()).filter(node => node.parents.length === 0);
      
      // 从根节点开始，广度优先遍历计算层级
      const queue = [...rootNodes];
      while (queue.length > 0) {
        const currentNode = queue.shift();
        
        // 处理所有子节点
        for (const childId of currentNode.children) {
          const childNode = nodeMap.get(childId);
          if (childNode) {
            // 子节点的层级是父节点层级+1
            const newLevel = currentNode.level + 1;
            // 取最大层级（如果有多个父节点）
            if (newLevel > childNode.level) {
              childNode.level = newLevel;
              levelMap.set(childId, newLevel);
              // 将子节点添加到队列中继续处理
              queue.push(childNode);
            }
          }
        }
      }
    };
    
    calculateLevels();
    
    return { nodeMap, levelMap };
  };
  
  // 应用层次化布局前的预处理
  const { nodeMap, levelMap } = buildHierarchicalGraph(elements.nodes, elements.edges);
  
  const elkNodes = elements.nodes.map(node => {
    const level = levelMap.get(node.id) || 0;
    return {
      id: node.id,
      width: 140, // 节点更宽
      height: 100, // 节点更高
      // 添加布局约束，确保节点顺序和层级
      layoutOptions: {
        'nodeLayering.layerId': level,
        'alignmentRegion': nodeMap.get(node.id).children.length > 0 ? 'TOP' : 'CENTER',
        'priority': nodeMap.get(node.id).children.length > 0 ? 10 : 1 // 优先布局有子节点的节点
      }
    };
  });
  
  const elkEdges = elements.edges.map(edge => ({
    id: edge.id,
    sources: [edge.source],
    targets: [edge.target]
  }));
  
  const graph = {
    id: 'root',
    children: elkNodes,
    edges: elkEdges
  };
  
  try {
    console.log('开始计算布局...');
    const layoutedGraph = await elk.layout(graph);
    console.log('布局计算完成:', layoutedGraph);
    
    // 更新节点位置
    const layoutedNodes = elements.nodes.map(node => {
      const layoutNode = layoutedGraph.children.find(n => n.id === node.id);
      if (layoutNode) {
        return {
          ...node,
          position: {
            x: layoutNode.x,
            y: layoutNode.y
          }
        };
      }
      return node;
    });
    
    // 优化边，使用正交路径，避免交叉
    const optimizedEdges = elements.edges.map(edge => {
      return {
        ...edge,
        type: 'step', // 使用step而不是smoothstep，减少交叉
        style: {
          ...edge.style,
          // 增加边缘半径，使曲线更平滑
          radius: 20
        }
      };
    });
    
    console.log('布局后的节点:', layoutedNodes);
    return {
      nodes: layoutedNodes,
      edges: optimizedEdges
    };
  } catch (error) {
    console.error('布局计算错误:', error);
    return elements;
  }
};

// 记录当前选中的节点
const selectedNode = ref(null);
// 编辑状态的节点（用于保存修改）
const editedNode = ref({});

// 处理节点点击事件
const onNodeClick = (event) => {
  console.log('节点点击事件:', event);
  const node = event.node;
  
  // 更新选中节点
  selectedNode.value = {
    ...node.data,
    id: node.id,
    node_type: node.type
  };
  
  // 初始化编辑状态的节点（深拷贝）
  editedNode.value = {
    ...JSON.parse(JSON.stringify(node.data)),
    id: node.id,
    node_type: node.type,
    // 确保字段存在，即使原数据中没有
    status: node.data.status || 'to_be_confirm',
    node_content: node.data.node_content || '',
    source_content: node.data.source_content || '',
    inference_relationship: node.data.inference_relationship || '',
    prompt_template: node.data.prompt_template || '',
    response_template: node.data.response_template || ''
  };
  
  console.log('选中节点:', selectedNode.value);
  console.log('编辑节点:', editedNode.value);
};

// 批准节点
const approveNode = async (nodeId) => {
  const updated = treeStore.updateNodeStatus(nodeId, 'confirm');
  if (updated) {
    console.log('节点状态已更新为已确认:', nodeId);
    await loadTree();
  }
};

// 拒绝节点
const rejectNode = async (nodeId) => {
  const updated = treeStore.updateNodeStatus(nodeId, 'reject');
  if (updated) {
    console.log('节点状态已更新为已拒绝:', nodeId);
    await loadTree();
  }
};

// 跳转到专家聊天页面
const navigateToExpertChat = () => {
  router.push('/expert-chat');
};

// 处理可视化按钮点击
const handleVisualization = () => {
  // 已在可视化页面，无需操作
};

// 选择树
const selectTree = async (treeId) => {
  try {
    // 先清空当前状态
    elements.value = [];
    setNodes([]);
    setEdges([]);
    selectedNode.value = null;
    editedNode.value = {};
    
    // 等待DOM更新
    await nextTick();
    
    // 设置新的树ID
    treeStore.setCurrentTree(treeId);
    
    // 加载新树
    await loadTree();
  } catch (error) {
    console.error('切换决策树失败:', error);
    ElMessage.error('切换决策树失败');
  }
};

// 加载树数据
const loadTree = async () => {
  try {
    loading.value = true;
    console.log('开始加载决策树...');
    
    // 获取treeId
    const currentTreeId = treeStore.currentTreeId || route.params.treeId;
    if (!currentTreeId) {
      throw new Error('未找到树ID');
    }
    
    console.log('正在加载树ID:', currentTreeId);
    const treeData = await treeStore.getTreeById(currentTreeId);
    
    // 新增：如果后端数据有 tree 字段（嵌套结构），递归转换为树形结构
    let nestedTree = null;
    if (treeData && treeData.tree) {
      nestedTree = convertBackendTree(treeData.tree);
      console.log('递归转换后的嵌套树形结构:', nestedTree);
      // 你可以在这里将 nestedTree 传递给递归渲染组件，或做进一步处理
    }
    
    if (!treeData) {
      console.error(`无法获取ID为'${currentTreeId}'的决策树数据`);
      ElMessage.warning(`无法获取ID为'${currentTreeId}'的决策树数据，将使用模拟数据`);
      
      // 使用模拟数据
      const mockData = mockDecisionTree;
      const { nodes: transformedNodes, edges: transformedEdges } = transformTree(mockData);
      
      // 应用布局
      const layoutedElements = await applyAutoLayout({
        nodes: transformedNodes,
        edges: transformedEdges
      });
      
      // 一次性设置所有元素
      elements.value = [...layoutedElements.nodes, ...layoutedElements.edges];
      setNodes(layoutedElements.nodes);
      setEdges(layoutedElements.edges);
      
      console.log('使用模拟数据加载完成');
      
      // 等待DOM更新后调整视图
      await nextTick();
      fitView();
      
      loading.value = false;
      return;
    }
    
    // 转换树数据为节点和边
    const { nodes: transformedNodes, edges: transformedEdges } = transformTree(treeData);
    
    // 应用布局
    const layoutedElements = await applyAutoLayout({
      nodes: transformedNodes,
      edges: transformedEdges
    });
    
    // 一次性设置所有元素
    elements.value = [...layoutedElements.nodes, ...layoutedElements.edges];
    setNodes(layoutedElements.nodes);
    setEdges(layoutedElements.edges);
    
    console.log('树数据加载完成');
    
    // 等待DOM更新后调整视图
    await nextTick();
    fitView();
    
  } catch (error) {
    console.error('加载决策树失败:', error);
    console.error('错误详情:', {
      message: error.message,
      stack: error.stack
    });
    
    // 使用模拟数据
    const mockData = mockDecisionTree;
    const { nodes: transformedNodes, edges: transformedEdges } = transformTree(mockData);
    
    // 应用布局
    const layoutedElements = await applyAutoLayout({
      nodes: transformedNodes,
      edges: transformedEdges
    });
    
    // 一次性设置所有元素
    elements.value = [...layoutedElements.nodes, ...layoutedElements.edges];
    setNodes(layoutedElements.nodes);
    setEdges(layoutedElements.edges);
    
    console.log('使用模拟数据加载完成');
    
    // 等待DOM更新后调整视图
    await nextTick();
    fitView();
    
    ElMessage.warning('加载决策树失败，已使用模拟数据');
  } finally {
    loading.value = false;
    console.log('加载决策树流程结束');
  }
};

// 监听树ID变化
watch(() => treeStore.currentTreeId, async (newTreeId, oldTreeId) => {
  console.log('树ID变化:', { newTreeId, oldTreeId });
  if (newTreeId && newTreeId !== oldTreeId) {
    await selectTree(newTreeId);
  }
}, { immediate: false });

// 保存节点修改
const saveNodeChanges = async () => {
  if (!selectedNode.value || !editedNode.value) {
    ElMessage.warning('请先选择要编辑的节点');
    return;
  }
  
  try {
    console.log('准备保存节点数据:', editedNode.value);
    
    // 准备更新数据，确保包含必要的节点属性
    const updateData = {
      node_id: editedNode.value.id,
      node_type: editedNode.value.node_type,
      label: editedNode.value.label,
      status: editedNode.value.status,
      node_content: editedNode.value.node_content,
      source_content: editedNode.value.source_content,
      inference_relationship: editedNode.value.inference_relationship
    };
    
    // 根据节点类型添加特定属性
    if (editedNode.value.node_type === 'decision') {
      updateData.prompt_template = editedNode.value.prompt_template;
    } else if (editedNode.value.node_type === 'leaf') {
      updateData.response_template = editedNode.value.response_template;
    }
    
    // 更新树中的节点数据
    const nodeId = selectedNode.value.id;
    console.log('正在更新节点:', nodeId, updateData);
    const updated = treeStore.updateNode(nodeId, updateData);
    
    if (updated) {
      // 更新当前选中节点的数据
      selectedNode.value = { ...editedNode.value };
      
      // 准备发送给后端的数据
      const requestData = treeStore.prepareDataForBackend();
      
      if (!requestData) {
        ElMessage.error('准备树数据失败');
        return;
      }
      
      // 发送修改请求到后端
      try {
        // 打印完整的请求数据
        console.log('发送到后端的完整数据:', requestData);

        // 创建axios实例并设置请求头
        const instance = axios.create({
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          }
        });

        // 打印实际请求头
        const config = {
          url: '/api/decision_trees',
          method: 'put',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
          data: requestData
        };

        console.log('PUT请求配置:', config);

        const response = await instance.put('/api/decision_trees', requestData, config);
        
        // 打印响应数据
        console.log('后端响应数据:', {
          status: response.status,
          statusText: response.statusText,
          headers: response.headers,
          data: response.data
        });
        
        if (response.status === 200) {
          // 清除修改记录
          treeStore.clearModifications();
          
          // 重新获取最新的树数据
          console.log('开始重新获取最新的树数据...');
          const updatedTrees = await treeStore.fetchAllBackendTrees();
          console.log('获取到的最新树数据:', updatedTrees);
          
          // 重新加载当前树
          await loadTree();
          
          ElMessage.success('节点修改已保存并同步到后端');
        } else {
          throw new Error('后端响应状态码异常');
        }
      } catch (error) {
        console.error('向后端发送修改请求失败:', error);
        ElMessage.error('保存到后端失败: ' + error.message);
      }
    } else {
      console.error('节点更新失败，treeStore.updateNode返回false');
      ElMessage.error('保存节点修改失败');
    }
  } catch (error) {
    console.error('保存节点修改失败:', error);
    ElMessage.error('保存节点修改失败: ' + error.message);
  }
};

// 取消编辑
const cancelEdit = () => {
  if (selectedNode.value) {
    // 重置编辑状态为当前选中节点的原始数据
    editedNode.value = JSON.parse(JSON.stringify(selectedNode.value));
    ElMessage.info('已取消修改');
  }
};

// 导出修改数据
const exportModifications = () => {
  const modifiedData = treeStore.prepareDataForBackend();
  
  if (Object.keys(modifiedData.modifiedNodes).length === 0) {
    ElMessage.info('没有修改记录可导出');
    return;
  }
  
  // 将数据转换为JSON字符串
  const jsonStr = JSON.stringify(modifiedData, null, 2);
  
  // 创建Blob对象
  const blob = new Blob([jsonStr], { type: 'application/json' });
  
  // 创建下载链接
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `tree_modifications_${new Date().toISOString().replace(/[:.]/g, '-')}.json`;
  document.body.appendChild(a);
  a.click();
  
  // 清理
  setTimeout(() => {
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }, 0);
  
  ElMessage.success('修改数据已导出');
};

// 清除修改记录
const clearModifications = () => {
  ElMessageBox.confirm('确定要清除所有修改记录吗？此操作不可撤销。', '确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    treeStore.clearModifications();
    ElMessage.success('修改记录已清除');
  }).catch(() => {
    ElMessage.info('已取消操作');
  });
};

// 组件挂载时加载树
onMounted(async () => {
  try {
    // 测试API连接
    await testApiConnection();
    
    // 获取后端所有决策树
    await treeStore.fetchAllBackendTrees();
    
    const treeId = route.params.treeId || 'stress'; // 默认加载压力决策树
    console.log('组件挂载，初始treeId:', treeId);
    
    if (treeId) {
      treeStore.setCurrentTree(treeId);
      await loadTree();
    }
  } catch (error) {
    console.error('组件挂载时加载树失败:', error);
    ElMessage.error('初始化决策树失败');
  }
});

// 节点管理相关状态
const showAddNodeDialogFlag = ref(false);
const newNodeForm = ref({
  node_type: 'decision',
  label: '',
  node_content: '',
  parent_id: '',
  status: 'to_be_confirm',
  relationship: 'solid',
  edge_label: ''
});

// 获取可选的父节点列表
const availableParentNodes = computed(() => {
  return elements.value.filter(el => !el.source && el.type === 'decision');
});

// 显示新增节点对话框
const showAddNodeDialog = () => {
  newNodeForm.value = {
    node_type: 'decision',
    label: '',
    node_content: '',
    parent_id: '',
    status: 'to_be_confirm',
    relationship: 'solid',
    edge_label: ''
  };
  showAddNodeDialogFlag.value = true;
};

// 添加新节点
const addNewNode = async () => {
  try {
    if (!newNodeForm.value.label || !newNodeForm.value.parent_id) {
      ElMessage.warning('请填写完整的节点信息');
      return;
    }

    // 生成新节点ID
    const newNodeId = `node_${Date.now()}`;
    
    // 创建新节点
    const newNode = {
      node_id: newNodeId,
      node_type: newNodeForm.value.node_type,
      label: newNodeForm.value.label,
      node_content: newNodeForm.value.node_content,
      status: newNodeForm.value.status,
      children: []
    };

    // 更新树数据
    const updated = treeStore.addNode(newNode, newNodeForm.value.parent_id, {
      relationship: newNodeForm.value.relationship,
      edge_label: newNodeForm.value.edge_label
    });
    
    if (updated) {
      // 准备发送给后端的数据
      const treeData = treeStore.prepareDataForBackend();
      
      if (!treeData) {
        ElMessage.error('准备树数据失败');
        return;
      }
      
      // 发送修改请求到后端
      try {
        console.log('发送到后端的数据:', treeData);
        const response = await axios.put('/api/decision_trees', treeData);
        
        if (response.status === 200) {
          // 清除修改记录
          treeStore.clearModifications();
          
          // 重新获取最新的树数据
          await treeStore.fetchAllBackendTrees();
          
          // 重新加载当前树
          await loadTree();
          
          ElMessage.success('节点添加成功并同步到后端');
          showAddNodeDialogFlag.value = false;
        } else {
          throw new Error('后端响应状态码异常');
        }
      } catch (error) {
        console.error('向后端发送添加请求失败:', error);
        ElMessage.error('添加到后端失败: ' + error.message);
      }
    } else {
      ElMessage.error('节点添加失败');
    }
  } catch (error) {
    console.error('添加节点失败:', error);
    ElMessage.error('添加节点失败');
  }
};

// 测试API连接
const testApiConnection = async () => {
  try {
    // 使用API模块中新增的检测连接方法
    const apiClient = axios.create({
      baseURL: '/api',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      }
    });

    const response = await apiClient.get('/decision_trees');
    
    if (response.status === 200) {
      connectionStatus.value = {
        type: 'success',
        message: '连接成功',
        details: {
          status: response.status,
          statusText: response.statusText
        }
      };
      return true;
    } else {
      connectionStatus.value = {
        type: 'error',
        message: '连接失败',
        details: {
          status: response.status,
          statusText: response.statusText
        }
      };
      return false;
    }
  } catch (error) {
    console.error('测试后端连接时出错:', error);
    connectionStatus.value = {
      type: 'error',
      message: '连接检测失败: ' + error.message,
      details: { error: error.message }
    };
    return false;
  }
};

// 刷新后端决策树
const refreshBackendTrees = async () => {
  try {
    isRefreshing.value = true;
    connectionStatus.value = { type: 'info', message: '正在连接后端...' };
    
    // 记录开始时间用于计算请求耗时
    const startTime = new Date();
    console.log('开始刷新后端决策树流程:', startTime.toISOString());
    
    // 首先测试API连接
    console.log('测试API连接...');
    const isApiAvailable = await testApiConnection();
    const connectionTime = new Date() - startTime;
    console.log(`API连接测试完成，耗时: ${connectionTime}ms, 结果:`, isApiAvailable);
    console.log('连接状态详情:', connectionStatus.value);
    
    if (!isApiAvailable) {
      console.warn('后端API连接测试未通过，但仍将尝试获取决策树');
      ElMessage.warning('后端API连接异常，无法获取最新决策树');
      console.warn('后端API连接异常，将使用缓存或静态决策树');
    }
    
    // 无论API是否可用，都尝试获取决策树（可能会使用缓存的静态数据）
    console.log('开始获取决策树列表...');
    const fetchStartTime = new Date();
    const trees = await treeStore.fetchAllBackendTrees();
    const fetchTime = new Date() - fetchStartTime;
    console.log(`决策树获取完成，耗时: ${fetchTime}ms`);
    console.log('获取到的树数据:', {
      是否有数据: Boolean(trees),
      数据类型: typeof trees,
      是否数组: Array.isArray(trees),
      数据长度: Array.isArray(trees) ? trees.length : '非数组'
    });
    
    if (trees && trees.length > 0) {
      console.log(`成功获取 ${trees.length} 个后端决策树`);
      console.log('树ID列表:', trees.map(tree => tree.tree_id || 'missing-id'));
      ElMessage.success(`成功获取 ${trees.length} 个后端决策树`);
    } else {
      console.log('未获取到后端决策树，将使用静态数据');
      ElMessage.info('未获取到后端决策树，将使用静态数据');
    }
    
    // 记录整个流程的总耗时
    const totalTime = new Date() - startTime;
    console.log(`刷新后端决策树流程完成，总耗时: ${totalTime}ms`);
  } catch (error) {
    console.error('刷新后端决策树失败:', error);
    console.error('错误详情:', {
      消息: error.message,
      堆栈: error.stack,
      名称: error.name,
      代码: error.code
    });
    ElMessage.error('刷新后端决策树失败: ' + error.message);
  } finally {
    isRefreshing.value = false;
  }
};
</script>

<style scoped>
.decision-tree-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f7fa;
  color: #333;
  width: 100vw;
  overflow-x: hidden;
  margin: 0;
  padding: 0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  height: 60px;
}

.header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.icon-refresh:before {
  content: '↻';
  margin-right: 4px;
}

.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
  width: 100%;
  min-width: 0;
}

.sidebar {
  width: 200px;
  background-color: #fff;
  border-right: 1px solid #e8e8e8;
  overflow-y: auto;
}

.sidebar-title {
  font-size: 16px;
  font-weight: bold;
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.menu-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.menu-item {
  padding: 12px 16px;
  cursor: pointer;
  font-size: 14px;
  border-left: 3px solid transparent;
}

.menu-item:hover {
  background-color: #f5f5f5;
}

.menu-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
  border-left: 3px solid #1890ff;
}

.menu-category {
  padding: 12px 16px 6px;
  font-size: 13px;
  color: #888;
  font-weight: bold;
  margin-top: 10px;
  border-top: 1px solid #eee;
}

.content-wrapper {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-width: 0;
  width: 100%;
}

.tree-title {
  font-size: 18px;
  font-weight: bold;
  padding: 16px 16px 8px;
  border-bottom: 1px solid #e8e8e8;
}

.content-area {
  display: flex;
  flex: 1;
  overflow: hidden;
  width: 100%;
  min-width: 0;
}

.decision-area {
  flex: 7;
  overflow: auto;
  padding: 16px;
  min-width: 0;
  width: 100%;
}

.tree-container {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 16px;
  height: calc(100% - 32px);
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
  transition: all 0.3s ease;
}

.flow-container {
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 500px;
  position: relative;
  border-radius: 6px;
  overflow: hidden;
  background-color: #f9fafc;
  transition: all 0.3s ease;
}

.flow-canvas {
  width: 100%;
  height: 100%;
  position: absolute;
  left: 0;
  top: 0;
}

/* 美化节点样式 */
:deep(.vue-flow__node) {
  background: #409EFF;
  color: white;
  border-radius: 50%;
  width: 70px !important;
  height: 70px !important;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  font-size: 12px;
  padding: 8px;
  border: none;
  z-index: 1;
  overflow: hidden;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
  transition: all 0.3s;
}

:deep(.vue-flow__node:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.2);
}

:deep(.vue-flow__node.leaf) {
  background: #67C23A;
}

:deep(.vue-flow__node.selected) {
  box-shadow: 0 0 0 3px #ff6b6b, 0 4px 10px rgba(0, 0, 0, 0.15);
}

/* 改进边的样式 */
:deep(.vue-flow__edge-path) {
  stroke: #409EFF;
  stroke-width: 2;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.1));
  transition: stroke 0.3s, stroke-width 0.3s;
}

:deep(.vue-flow__edge:hover .vue-flow__edge-path) {
  stroke-width: 3;
  stroke: #1a73e8;
}

:deep(.vue-flow__edge.animated .vue-flow__edge-path) {
  stroke-dasharray: 5;
  animation: dashdraw 0.5s linear infinite;
  stroke: #f56c6c;
}

/* 箭头样式 */
:deep(.vue-flow__edge-arrow) {
  fill: #409EFF;
  stroke: none;
  transition: fill 0.3s;
}

:deep(.vue-flow__edge:hover .vue-flow__edge-arrow) {
  fill: #1a73e8;
}

:deep(.vue-flow__edge.animated .vue-flow__edge-arrow) {
  fill: #f56c6c;
}

/* 改进背景样式 */
:deep(.vue-flow__background) {
  background-color: #f9fafc;
}

:deep(.vue-flow__minimap) {
  background-color: white;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

:deep(.vue-flow__controls) {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  border-radius: 6px;
  background-color: white;
  border: 1px solid #ebeef5;
}

:deep(.vue-flow__controls-button) {
  background: white;
  border-bottom: 1px solid #ebeef5;
  color: #606266;
  transition: all 0.3s;
}

:deep(.vue-flow__controls-button:hover) {
  background: #f5f7fa;
  color: #409EFF;
}

/* 图例样式美化 */
.tree-legend {
  display: flex;
  justify-content: center;
  padding: 16px 0 0;
  border-top: 1px solid #ebeef5;
  margin-top: 20px;
  background-color: white;
  border-radius: 0 0 8px 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 8px 16px;
}

.legend-label {
  font-weight: bold;
  margin-right: 8px;
  color: #606266;
}

.legend-dot {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.legend-dot.decision {
  background-color: #409EFF;
}

.legend-dot.leaf {
  background-color: #67C23A;
}

.legend-line {
  display: inline-block;
  width: 24px;
  height: 2px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.legend-line.solid {
  background-color: #409EFF;
}

.legend-line.dashed {
  border-top: 2px dashed #f56c6c;
}

.legend-text {
  margin-right: 16px;
  font-size: 12px;
  color: #606266;
}

.node-panel {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 20px;
  height: calc(100% - 40px);
  overflow: auto;
}

.panel-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 6px;
  color: #409EFF;
}

.panel-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 12px;
}

.node-content {
  border-radius: 4px;
  padding: 0;
  margin-bottom: 16px;
}

.content-section {
  margin-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
}

.content-section h5 {
  font-size: 15px;
  color: #333;
  margin: 0 0 10px 0;
  font-weight: 600;
}

.edit-field {
  margin-bottom: 12px;
}

.form-control {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.5;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-control:focus {
  border-color: #409EFF;
  outline: 0;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

textarea.form-control {
  min-height: 80px;
  resize: vertical;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.btn-save {
  margin-right: 77px;
  background-color: #1890ff;
  color: white;
}

.btn-cancel {
  background-color: #f5f5f5;
  color: #333;
}

/* 面板操作按钮 */
.panel-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 12px;
}

.node-management {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.btn-export {
  background-color: #52c41a;
  color: white;
  flex: 1;
}

.btn-clear {
  background-color: #faad14;
  color: white;
  flex: 1;
}

/* 节点管理按钮样式 */
.btn-add {
  background-color: #52c41a;
  color: white;
  flex: 1;
}

.btn-delete {
  background-color: #f5222d;
  color: white;
  flex: 1;
}

.btn-delete:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}

/* 对话框样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.status-indicator {
  padding: 5px 10px;
  border-radius: 4px;
  font-size: 14px;
  display: inline-block;
}

.status-indicator.success {
  background-color: #dff0d8;
  color: #3c763d;
  border: 1px solid #d6e9c6;
}

.status-indicator.error {
  background-color: #f2dede;
  color: #a94442;
  border: 1px solid #ebccd1;
}

.status-indicator.warning {
  background-color: #fcf8e3;
  color: #8a6d3b;
  border: 1px solid #faebcc;
}

.status-indicator.info {
  background-color: #d9edf7;
  color: #31708f;
  border: 1px solid #bce8f1;
}

.loading-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-radius: 50%;
  border-top-color: #fff;
  animation: spin 1s ease-in-out infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 加载状态样式 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 6px;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409EFF;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.05);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.no-node-selected {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-style: italic;
  background-color: #f9fafc;
  border-radius: 6px;
  padding: 30px;
  text-align: center;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.no-node-selected:hover {
  color: #606266;
  background-color: #f0f2f5;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .content-area {
    flex-direction: column;
  }
  
  .decision-area, 
  .node-area {
    flex: none;
    width: 100%;
  }
  
  .node-area {
    border-left: none;
    border-top: 1px solid #e8e8e8;
    min-width: auto;
    max-width: none;
  }
}

/* 编辑字段样式 */
.edit-field {
  margin-bottom: 10px;
}

.form-control {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.5;
  transition: all 0.3s;
}

.form-control:focus {
  border-color: #1890ff;
  outline: 0;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

textarea.form-control {
  min-height: 60px;
  resize: vertical;
}

select.form-control {
  height: 36px;
  background-color: #fff;
}

.btn-save {
  margin-right: 77px;
  background-color: #1890ff;
  color: white;
}

.btn-cancel {
  background-color: #f5f5f5;
  color: #333;
}

/* 面板操作按钮 */
.panel-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 8px;
}

.btn-export {
  background-color: #52c41a;
  color: white;
  flex: 1;
}

.btn-clear {
  background-color: #faad14;
  color: white;
  flex: 1;
  margin-left: 0;
}

/* 节点管理按钮样式 */
.node-management {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.btn-add {
  background-color: #52c41a;
  color: white;
  flex: 1;
}

.btn-delete {
  background-color: #f5222d;
  color: white;
  flex: 1;
}

.btn-delete:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}

/* 对话框样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.status-indicator {
  padding: 5px 10px;
  border-radius: 4px;
  font-size: 14px;
  display: inline-block;
}

.status-indicator.success {
  background-color: #dff0d8;
  color: #3c763d;
  border: 1px solid #d6e9c6;
}

.status-indicator.error {
  background-color: #f2dede;
  color: #a94442;
  border: 1px solid #ebccd1;
}

.status-indicator.warning {
  background-color: #fcf8e3;
  color: #8a6d3b;
  border: 1px solid #faebcc;
}

.status-indicator.info {
  background-color: #d9edf7;
  color: #31708f;
  border: 1px solid #bce8f1;
}

.loading-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-radius: 50%;
  border-top-color: #fff;
  animation: spin 1s ease-in-out infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

:deep(.vue-flow__node-content) {
  width: 100%;
  max-width: 100%;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

:deep(.vue-flow__handle) {
  opacity: 0;
  width: 0;
  height: 0;
}

@keyframes dashdraw {
  from {
    stroke-dashoffset: 10;
  }
}

/* 添加节点进入/退出动画效果 */
:deep(.vue-flow__node-enter-active),
:deep(.vue-flow__node-leave-active) {
  transition: all 0.5s ease;
}

:deep(.vue-flow__node-enter-from),
:deep(.vue-flow__node-leave-to) {
  opacity: 0;
  transform: scale(0.5);
}

/* 添加边的进入/退出动画效果 */
:deep(.vue-flow__edge-enter-active),
:deep(.vue-flow__edge-leave-active) {
  transition: all 0.5s ease;
}

:deep(.vue-flow__edge-enter-from),
:deep(.vue-flow__edge-leave-to) {
  opacity: 0;
  stroke-dasharray: 10;
  stroke-dashoffset: 20;
}
</style> 
