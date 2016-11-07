select ei.affiliateId, p.passwordStateFlag, p.passwordExpirationDate, p.lastPasswordChangeDate,
pt.principalType, pt.principalTypeSDesc, a.firstName, a.middleName, a.lastName, p.principal, concat(p.principal, '@asu.edu') as principalScoped,
r.loginDiversionState
from Principal p, ctPrincipalType pt, AffiliateElectronicIdentity ei, Affiliate a
left join AffiliatePasswordResetEnrollment r using (affiliateId)
where p.principal = ?
and p.inactiveCode = 'A'
and p.principalType = pt.principalType
and pt.inactiveCode = 'A'
and p.electronicIdentityKey = ei.electronicIdentityKey
and ei.affiliateElectronicIdentityType = 1
and ei.inactiveCode = 'A'
and ei.affiliateId = a.affiliateId