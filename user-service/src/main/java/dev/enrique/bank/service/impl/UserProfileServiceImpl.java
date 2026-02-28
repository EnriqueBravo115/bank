package dev.enrique.bank.service.impl;

import dev.enrique.bank.dto.request.PatchFinancialProfileRequest;
import dev.enrique.bank.dto.request.PatchPersonalInfoRequest;
import dev.enrique.bank.dto.response.FinancialProfileResponse;
import dev.enrique.bank.dto.response.PersonalInfoResponse;
import dev.enrique.bank.service.UserProfileService;

public class UserProfileServiceImpl implements UserProfileService {
    @Override
    public PersonalInfoResponse updatePersonalInfo(PatchPersonalInfoRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updatePersonalInfo'");
    }

    @Override
    public FinancialProfileResponse updateEmployment(PatchFinancialProfileRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateEmployment'");
    }
}
