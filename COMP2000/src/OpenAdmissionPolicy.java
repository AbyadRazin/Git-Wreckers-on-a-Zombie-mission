/** First come, first served. Building still enforces its capacity. */
public final class OpenAdmissionPolicy implements AdmissionPolicy {
    @Override
    public boolean allows(Human person, Building building) {
        return true;
    }

    @Override
    public String describe() {
        return "Open shelter";
    }
}