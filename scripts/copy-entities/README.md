## Prerequisite to Run the script

- Create an API Key with admin permissions
- All the Secret Managers needs to be migrated manually. <br>
  Cases when this is not needed
    - If the SM are at account level then there is no need to do the same
    - If the migration is being done in the same org and the SM are present at the org level then this is not needed


## Limitations of the Script

1. This script won’t migrate any inline secrets stored in any secret manager.
2. All the remote Pipelines will be moved to Inline.
3. All the remote templates would be move to Inline.
4. If the SM is not migrated then the secrets present in that SM will also be not migrated.
5. Make sure that the identifier remains the same while creating the prerequisites data.


## Entities Supported

- Secrets
- Connectors
- Service
- Environment
- Template
- Pipeline

### Order of Execution of the Script


Secrets  -> Connector -> Service -> Environment -> Template -> Pipeline


### Command to Execute this script

python3 script.py accountIdentifier from_projectIdentifier to_ProjectIdentifier from_orgIdentifier to_orgIdentifier x-api-key
