package dev.enrique.bank.service;

import dev.enrique.bank.dto.request.PatchFinancialProfileRequest;
import dev.enrique.bank.dto.request.PatchPersonalInfoRequest;
import dev.enrique.bank.dto.response.FinancialProfileResponse;
import dev.enrique.bank.dto.response.PersonalInfoResponse;

public interface UserProfileService {
    PersonalInfoResponse updatePersonalInfo(PatchPersonalInfoRequest request);

    FinancialProfileResponse updateEmployment(PatchFinancialProfileRequest request);
}
