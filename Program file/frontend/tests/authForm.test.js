import assert from 'node:assert/strict'
import test from 'node:test'

import {
  getLoginErrorMessage,
  isLoginFormValid,
  isRegistrationFormValid,
} from '../src/utils/authForm.js'

const validLogin = { roleCode: 'USER', phone: '13800000001', password: '123456', employeeNo: '' }

test('USER and CLUB login require role, valid phone and password', () => {
  assert.equal(isLoginFormValid(validLogin), true)
  assert.equal(isLoginFormValid({ ...validLogin, roleCode: 'CLUB' }), true)
  assert.equal(isLoginFormValid({ ...validLogin, roleCode: '' }), false)
  assert.equal(isLoginFormValid({ ...validLogin, phone: '1380000000' }), false)
  assert.equal(isLoginFormValid({ ...validLogin, password: '' }), false)
})

test('management login additionally requires the matching employee number format', () => {
  assert.equal(isLoginFormValid({ ...validLogin, roleCode: 'EVENT_ADMIN', employeeNo: '' }), false)
  assert.equal(isLoginFormValid({ ...validLogin, roleCode: 'EVENT_ADMIN', employeeNo: '0001' }), true)
  assert.equal(isLoginFormValid({ ...validLogin, roleCode: 'EVENT_ADMIN', employeeNo: 'EA0001' }), false)
  assert.equal(isLoginFormValid({ ...validLogin, roleCode: 'ADMIN', employeeNo: '0001' }), true)
  assert.equal(isLoginFormValid({ ...validLogin, roleCode: 'ADMIN', employeeNo: 'SA0001' }), false)
})

const validRegistration = {
  roleCode: 'USER',
  username: '测试用户',
  realName: '测试姓名',
  clubName: '',
  phone: '13800000001',
  password: '123456',
  confirmPassword: '123456',
}

test('USER registration does not require real name while CLUB still does', () => {
  assert.equal(isRegistrationFormValid(validRegistration), true)
  assert.equal(isRegistrationFormValid({ ...validRegistration, realName: '' }), true)
  assert.equal(isRegistrationFormValid({ ...validRegistration, roleCode: '' }), false)
  assert.equal(isRegistrationFormValid({ ...validRegistration, roleCode: 'CLUB' }), false)
  assert.equal(isRegistrationFormValid({ ...validRegistration, roleCode: 'CLUB', clubName: '测试俱乐部' }), true)
  assert.equal(isRegistrationFormValid({ ...validRegistration, roleCode: 'CLUB', clubName: '测试俱乐部', realName: '' }), false)
})

test('registration remains disabled for invalid formats and mismatched passwords', () => {
  assert.equal(isRegistrationFormValid({ ...validRegistration, phone: '123' }), false)
  assert.equal(isRegistrationFormValid({ ...validRegistration, password: '12345', confirmPassword: '12345' }), false)
  assert.equal(isRegistrationFormValid({ ...validRegistration, confirmPassword: '654321' }), false)
})

test('known login errors are mapped to explicit user messages and unknown errors are preserved', () => {
  assert.equal(getLoginErrorMessage({ message: '手机号不存在' }), '该账号未注册')
  assert.equal(getLoginErrorMessage({ message: '密码错误' }), '密码错误，请重新输入')
  assert.equal(getLoginErrorMessage({ message: '所选身份与账号不匹配' }), '所选身份与账号不匹配')
  assert.equal(getLoginErrorMessage({ message: '工号与账号不匹配' }), '工号错误，与该账号不匹配')
  assert.equal(getLoginErrorMessage({ message: '账号尚未启用' }), '账号尚未启用')
})
