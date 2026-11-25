import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import DashboardLayout from '../views/DashboardLayout.vue'
import ElectricCompanyDataView from '../views/ElectricCompanyDataView.vue'
import PendingCompanyDataView from '../views/PendingCompanyDataView.vue'
import ModelTrainingView from '../views/ModelTrainingView.vue'
import ModelComparisonView from '../views/ModelComparisonView.vue'
import FederatedLearningView from '../views/FederatedLearningView.vue'
import CommonCustomerView from '../views/CommonCustomerView.vue'
import FinancialDataNoticeView from '../views/FinancialDataNoticeView.vue'
import CompanyLoginView from '../views/CompanyLoginView.vue'
import CompanyRegisterView from '../views/CompanyRegisterView.vue'
import CompanyDashboardLayout from '../views/CompanyDashboardLayout.vue'
import CompanyAssessmentDataView from '../views/CompanyAssessmentDataView.vue'
import CompanyMissingDataView from '../views/CompanyMissingDataView.vue'
import CompanyMaterialView from '../views/CompanyMaterialView.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginView },
  { path: '/financial/login', redirect: '/login' },
  { path: '/register', component: RegisterView },
  { path: '/financial/register', redirect: '/register' },
  { path: '/company/login', component: CompanyLoginView },
  { path: '/company/register', component: CompanyRegisterView },
  {
    path: '/dashboard',
    component: DashboardLayout,
    children: [
      { path: '', redirect: '/dashboard/customer-overview' },
      { path: 'customer-overview', component: () => import('../views/CustomerOverviewView.vue') },
      { path: 'electric-company-data', component: ElectricCompanyDataView },
      { path: 'pending-company-data', component: PendingCompanyDataView },
      { path: 'model-training', component: ModelTrainingView },
      { path: 'dynamic-update', component: () => import('../views/DynamicUpdateView.vue') },
      { path: 'model-comparison', component: ModelComparisonView },
      { path: 'federated-learning', component: FederatedLearningView },
      { path: 'common-customers', component: CommonCustomerView },
      { path: 'data-notices', component: FinancialDataNoticeView }
    ]
  },
  {
    path: '/company/dashboard',
    component: CompanyDashboardLayout,
    children: [
      { path: '', redirect: '/company/dashboard/history-data' },
      {
        path: 'history-data',
        component: CompanyAssessmentDataView,
        meta: { datasetType: 'history' }
      },
      {
        path: 'pending-data',
        component: CompanyAssessmentDataView,
        meta: { datasetType: 'pending' }
      },
      { path: 'missing-data', component: CompanyMissingDataView },
      { path: 'materials', component: CompanyMaterialView }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const financialPublicPages = ['/login', '/register', '/financial/login', '/financial/register']
  const companyPublicPages = ['/company/login', '/company/register']
  const financialUser = localStorage.getItem('financialUser')
  const companyUser = localStorage.getItem('companyUser')

  if (to.path.startsWith('/company/dashboard') && !companyUser) {
    return '/company/login'
  }
  if (to.path.startsWith('/dashboard') && !financialUser) {
    return '/login'
  }
  if (financialPublicPages.includes(to.path) && financialUser) {
    return '/dashboard/customer-overview'
  }
  if (companyPublicPages.includes(to.path) && companyUser) {
    return '/company/dashboard/history-data'
  }
  return true
})

export default router
