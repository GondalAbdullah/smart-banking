package com.bank.service;

/** Fields to change. A null or blank value means "keep the current value". */
public record CustomerUpdate(String fullName, String email, String phone, String address) {

}