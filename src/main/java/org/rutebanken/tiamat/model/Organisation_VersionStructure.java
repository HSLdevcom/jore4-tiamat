package org.rutebanken.tiamat.model;

import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;

@MappedSuperclass
public class Organisation_VersionStructure extends DataManagedObjectStructure {
    private String privateCode;

    private String companyNumber;

    private String name;

    @Enumerated(EnumType.STRING)
    protected OrganisationTypeEnumeration organisationType;

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "legal_name_value")),
            @AttributeOverride(name = "lang", column = @Column(name = "legal_name_lang", length = 5))
    })
    @Embedded
    private EmbeddableMultilingualString legalName;

    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    protected Contact contactDetails;

    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    protected Contact privateContactDetails;

    public String getPrivateCode() {
        return privateCode;
    }

    public void setPrivateCode(String privateCode) {
        this.privateCode = privateCode;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OrganisationTypeEnumeration getOrganisationType() {
        return organisationType;
    }

    public void setOrganisationType(OrganisationTypeEnumeration organisationType) {
        this.organisationType = organisationType;
    }

    public EmbeddableMultilingualString getLegalName() {
        return legalName;
    }

    public void setLegalName(EmbeddableMultilingualString legalName) {
        this.legalName = legalName;
    }

    public Contact getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(Contact contactDetails) {
        this.contactDetails = contactDetails;
    }

    public Contact getPrivateContactDetails() {
        return privateContactDetails;
    }

    public void setPrivateContactDetails(Contact privateContactDetails) {
        this.privateContactDetails = privateContactDetails;
    }
}
