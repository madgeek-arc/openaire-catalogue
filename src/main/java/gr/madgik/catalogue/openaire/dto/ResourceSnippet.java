package gr.madgik.catalogue.openaire.dto;

import java.net.URL;
import java.util.List;

public class ResourceSnippet {

    String id;
    URL image;
    URL longImage;
    String paymentTitle;
    String name;
    String description;
    String tagline;
    URL logo;
    List<UiVocabulary> portfolios;
    String pitch;
    String label;
    List<UiVocabulary> user;

    public ResourceSnippet() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public URL getImage() {
        return image;
    }

    public void setImage(URL image) {
        this.image = image;
    }

    public URL getLongImage() {
        return longImage;
    }

    public void setLongImage(URL longImage) {
        this.longImage = longImage;
    }

    public String getPaymentTitle() {
        return paymentTitle;
    }

    public void setPaymentTitle(String paymentTitle) {
        this.paymentTitle = paymentTitle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public URL getLogo() {
        return logo;
    }

    public void setLogo(URL logo) {
        this.logo = logo;
    }

    public List<UiVocabulary> getPortfolios() {
        return portfolios;
    }

    public void setPortfolios(List<UiVocabulary> portfolios) {
        this.portfolios = portfolios;
    }

    public String getPitch() {
        return pitch;
    }

    public void setPitch(String pitch) {
        this.pitch = pitch;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<UiVocabulary> getUser() {
        return user;
    }

    public void setUser(List<UiVocabulary> user) {
        this.user = user;
    }
}
