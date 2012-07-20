select ei.affiliateId, p.passwordStateFlag, p.passwordExpirationDate, p.lastPasswordChangeDate,
pt.principalType, pt.principalTypeSDesc, a.firstName, a.middleName, a.lastName
from Principal p, ctPrincipalType pt, AffiliateElectronicIdentity ei, Affiliate a
where p.principal = :username
and p.inactiveCode = 'A'
and p.principalType = pt.principalType
and pt.inactiveCode = 'A'
and p.electronicIdentityKey = ei.electronicIdentityKey
and ei.affiliateElectronicIdentityType = 1
and ei.inactiveCode = 'A'
and ei.affiliateId = a.affiliateId