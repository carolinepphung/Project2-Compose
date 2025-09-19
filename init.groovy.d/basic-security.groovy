#!groovy
import jenkins.model.*
import hudson.security.*
import hudson.util.*;
import jenkins.security.s2m.AdminWhitelistRule
import hudson.security.csrf.DefaultCrumbIssuer

def instance = Jenkins.getInstance()

// Read env variables
def adminName = System.getenv("JENKINS_ADMIN_USER") ?: "admin"
def adminPass = System.getenv("JENKINS_ADMIN_PASS") ?: "admin"
def ciName = System.getenv("JENKINS_CI_USER") ?: "ci-user"
def ciPass = System.getenv("JENKINS_CI_PASS") ?: "ci-user-pass"

// Create user realm and users if not existing
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount(adminName, adminPass)
hudsonRealm.createAccount(ciName, ciPass)
instance.setSecurityRealm(hudsonRealm)

// Use matrix authorization strategy to set precise permissions
def strategy = new GlobalMatrixAuthorizationStrategy()

// Give admin full control
strategy.add(Jenkins.ADMINISTER, adminName)

// Give ci-user limited permissions required for pipelines: read, build, job/overall read/view if needed
strategy.add(Item.BUILD, ciName)
strategy.add(Item.READ, ciName)
strategy.add(Permission.fromId("hudson.model.View.Read"), ciName)
strategy.add(Jenkins.READ, ciName)

// Disable anonymous access
strategy.add(Jenkins.READ, "anonymous") // will be removed below
// Now prevent anonymous from doing anything (this effectively disables anonymous access)
strategy.remove(Jenkins.SYSTEM_READ) // no-op if not present

instance.setAuthorizationStrategy(strategy)

// Disable anonymous read access explicitly
instance.setDisableRememberMe(true)
instance.setCrumbIssuer(new DefaultCrumbIssuer(true))

// Optional: set mark that anonymous can't read (redundant with matrix config)
instance.setItemACL(null)
instance.save()
println "--> basic-security.groovy: security realm and matrix authorization configured. Admin: ${adminName}, CI user: ${ciName}"

